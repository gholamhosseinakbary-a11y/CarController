package com.carcontroller.app

// =====================================================================
// AppModel.kt — وضعیت سراسری + منطق دو حالت اتصال (سرور VPS / اکسس‌پوینت)
// ---------------------------------------------------------------------
// هر فرمان/تنظیم دو پیاده‌سازی دارد:
//   حالت vps → فرمان تایپ‌شدهٔ REST سرور + انتظار ACK دستگاه
//   حالت ap  → اندپوینت مستقیم /api/* فریمور (مرحلهٔ ۴)
// =====================================================================

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.WebSocket
import org.json.JSONArray
import org.json.JSONObject

object AppModel : CoroutineScope by MainScope() {

    // ---------------- وضعیت UI ----------------
    var mode by mutableStateOf("")                 // "vps" | "ap"
    var connected by mutableStateOf(false)
    var lastError by mutableStateOf("")
    var state by mutableStateOf(DeviceState())
    var toast by mutableStateOf("")
    var busy by mutableStateOf(false)

    var deviceId by mutableStateOf("")
    var deviceName by mutableStateOf("")
    var userName by mutableStateOf("")
    var isAdmin by mutableStateOf(false)
    var devices by mutableStateOf<List<JSONObject>>(emptyList())
    var wsConnected by mutableStateOf(false)

    private var pollJob: Job? = null
    private var ws: WebSocket? = null

    fun vps(): VpsClient = VpsClient(Prefs.vpsUrl, Prefs.token)
    fun ap(): ApClient = ApClient(Prefs.apHost)

    fun loadFromPrefs() {
        mode = Prefs.mode
        deviceId = Prefs.deviceId
        deviceName = Prefs.deviceName
        userName = Prefs.userName
    }

    fun showToast(msg: String) {
        toast = msg
        launch { delay(4500); if (toast == msg) toast = "" }
    }

    // ---------------- خطای فارسی ----------------
    private fun friendly(e: Exception): String = when (e) {
        is ApiException -> when (e.code) {
            401 -> "نشست منقضی شده — دوباره وارد شوید"
            403 -> "این دستگاه (MAC) مجاز نیست — ابتدا کد فعال‌سازی را وارد کنید"
            404 -> "دستگاه یافت نشد"
            400 -> e.message ?: "خطای درخواست"
            else -> e.message ?: "خطای سرور"
        }
        else -> "ارتباط برقرار نشد؛ اتصال شبکه/سرور/وای‌فای را بررسی کنید"
    }

    // ---------------- polling وضعیت ----------------
    fun startPolling() {
        stopPolling()
        pollJob = launch {
            while (true) {
                refresh()
                delay(if (mode == "ap") 4000L else 6000L)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    suspend fun refresh() {
        try {
            if (mode == "ap") {
                state = DeviceStateParse.fromApInfo(ap().info())
            } else {
                val r = vps().snapshot(deviceId)
                val st = r.obj("lastStat") ?: JSONObject()
                val d = DeviceStateParse.fromVpsSnapshot(st)
                d.online = r.obj("device").bool("online")
                state = d
            }
            connected = true
            lastError = ""
        } catch (e: Exception) {
            connected = false
            lastError = friendly(e)
        }
    }

    // ---------------- WebSocket زنده (سرور) ----------------
    fun startWs() {
        stopWs()
        if (mode != "vps" || Prefs.token.isEmpty() || deviceId.isEmpty()) return
        try {
            ws = vps().openWebSocket(
                onMsg = { txt ->
                    try {
                        val m = JSONObject(txt)
                        when (m.str("type")) {
                            "stat", "ack", "dev_online", "dev_offline" -> launch { refresh() }
                            "evt" -> {
                                val d = m.obj("data")
                                showToast(evtText(d.str("k"), d.str("d")))
                                launch { refresh() }
                            }
                        }
                    } catch (_: Exception) { }
                },
                onState = { ok -> wsConnected = ok }
            )
        } catch (_: Exception) { }
    }

    fun stopWs() {
        try { ws?.close(1000, "bye") } catch (_: Exception) { }
        ws = null
        wsConnected = false
    }

    private fun evtText(k: String, d: String): String = when (k) {
        "rfid" -> "رخداد RFID: $d"
        "sms_in" -> "پیامک دریافتی (مدیریت از تب پیامک‌ها)"
        "alarm_on" -> "آژیر فعال شد"
        "alarm_off" -> "آژیر خاموش شد"
        "lock" -> "قفل خودرو: $d"
        "safe_zone" -> "هشدار محدودهٔ امن!"
        else -> "رخداد: $k $d"
    }

    // ---------------- ابزار اجرای فرمان ----------------

    /** انتظار برای ACK فرمان از دستگاه (حالت سرور). null = هنوز نرسیده */
    private suspend fun waitForAck(cmdId: String, tries: Int = 12): Pair<Boolean, String?>? {
        for (i in 0 until tries) {
            delay(800)
            try {
                val cmd = vps().cmdStatus(deviceId, cmdId).obj("cmd") ?: continue
                if (!cmd.bool("acked")) continue
                val res = cmd.opt("result")
                if (res is JSONObject) return Pair(res.bool("ok", true), res.str("e").ifEmpty { null })
                if (res is String) return Pair(!res.startsWith("error:"), res.removePrefix("error:"))
                return Pair(true, null)
            } catch (_: Exception) { }
        }
        return null
    }

    /** اجرای فرمان از طریق /cmd (حالت سرور) یا اندپوینت مستقیم (حالت AP) */
    private suspend fun run(
        label: String,
        vpsCmd: Pair<String, JSONObject>? = null,
        apRun: (suspend ApClient.() -> JSONObject)? = null,
        waitAck: Boolean = true
    ): String {
        busy = true
        try {
            if (mode == "vps") {
                if (vpsCmd == null) return "$label: در حالت سرور در دسترس نیست"
                val r = vps().sendCmdRaw(deviceId, vpsCmd.first, vpsCmd.second)
                val cmdId = r.str("cmdId")
                if (!r.bool("delivered")) {
                    return "$label: در صف قرار گرفت (دستگاه آفلاین) — پس از اتصال اجرا می‌شود"
                }
                if (waitAck && cmdId.isNotEmpty()) {
                    val ack = waitForAck(cmdId)
                    if (ack != null) {
                        return if (ack.first) "$label ✔ با موفقیت اجرا شد"
                        else "$label ✖ " + (ack.second ?: "خطا در دستگاه")
                    }
                }
                return "$label ✔ ارسال شد"
            } else {
                if (apRun == null) return "$label: در حالت اکسس‌پوینت در دسترس نیست"
                val res = ap().apRun()
                val ok = res.bool("ok") || res.bool("success")
                return if (ok) "$label ✔"
                else "$label ✖ " + res.str("e", res.str("message"))
            }
        } catch (e: Exception) {
            return "$label ✖ " + friendly(e)
        } finally {
            busy = false
            launch { refresh() }
        }
    }

    /** اجرای مسیر تایپ‌شدهٔ سرور (control/settings/actions) + ACK */
    private suspend fun typedRun(label: String, kind: String, action: String, body: JSONObject): String {
        busy = true
        try {
            val r = vps().typed(deviceId, kind, action, body)
            val cmdId = r.str("cmdId")
            if (!r.bool("delivered")) return "$label: در صف قرار گرفت (دستگاه آفلاین)"
            if (cmdId.isNotEmpty()) {
                val ack = waitForAck(cmdId, 10)
                if (ack != null) {
                    return if (ack.first) "$label ✔"
                    else "$label ✖ " + (ack.second ?: "خطا در دستگاه")
                }
            }
            return "$label ✔ ارسال شد"
        } catch (e: Exception) {
            return "$label ✖ " + friendly(e)
        } finally {
            busy = false
            launch { refresh() }
        }
    }

    // ---------------- کنترل‌ها ----------------

    /** دکمه‌های اصلی صفحهٔ اول — مثل نزدیک‌کردن تگ به RC522 */
    suspend fun rfidStage(stage: Int): String {
        val label = if (stage == 1)
            "مرحلهٔ ۱ تگ RFID (رفع قفل + باز شدن درب‌ها)"
        else
            "مرحلهٔ ۲ تگ RFID (استارت خودرو)"
        return if (mode == "vps") {
            typedRun(label, "actions", "rfid-sim", JSONObject().put("stage", stage))
        } else {
            run(label, apRun = { rfidSim(stage) })
        }
    }

    suspend fun setOutput(pin: Int, value: Boolean): String {
        val nm = state.names.getOrElse(pin) { "" }.ifEmpty { "خروجی ${pin + 1}" }
        // [FIX] به‌روزرسانی خوش‌بینانهٔ نمایش: کارت همان لحظه تغییر می‌کند؛
        // refresh بعد از فرمان، مقدار واقعی دستگاه را تأیید/اصلاح می‌کند.
        if (pin != 1) {   // صندوق عقب پالسی است؛ فلش یک‌ثانیه‌ای صفحهٔ اصلی آن را نشان می‌دهد
            val cur = state.outputs.toMutableList()
            if (pin in cur.indices) {
                cur[pin] = value
                state = state.copy(outputs = cur)
            }
        }
        return if (mode == "vps")
            run(nm, vpsCmd = "out" to JSONObject().put("i", pin).put("v", if (value) 1 else 0))
        else
            run(nm, apRun = { toggle(pin) })
    }

    suspend fun doLock(): String =
        run("قفل خودرو (قطع سوئیچ/پمپ)", vpsCmd = "lock" to JSONObject(), apRun = { lock() })

    suspend fun doUnlock(): String =
        run("رفع قفل خودرو", vpsCmd = "unlock" to JSONObject(), apRun = { unlock() })

    suspend fun doAlarm(on: Boolean): String {
        val label = if (on) "روشن‌کردن آژیر" else "خاموش‌کردن آژیر"
        return if (mode == "vps")
            run(label, vpsCmd = (if (on) "alarm_on" else "alarm_off") to JSONObject(), apRun = { alarm(on) })
        else
            run(label, apRun = { alarm(on) })
    }

    suspend fun reboot(): String =
        run("ری‌استارت دستگاه", vpsCmd = "reboot" to JSONObject(), apRun = { reboot() }, waitAck = false)

    // ---------------- تنظیمات ----------------

    suspend fun renameOutput(pin: Int, name: String): String =
        if (mode == "vps")
            typedRun("نام خروجی ${pin + 1}", "settings", "output-name",
                JSONObject().put("index", pin).put("name", name.trim()))
        else
            run("نام خروجی ${pin + 1}", apRun = { setOutputName(pin, name) })

    suspend fun saveSignal(v: Int): String =
        if (mode == "vps")
            typedRun("آستانهٔ سیگنال", "settings", "signal", JSONObject().put("value", v))
        else
            run("آستانهٔ سیگنال", apRun = { setSignal(v) })

    suspend fun saveWeakAuto(on: Boolean): String =
        if (mode == "vps")
            typedRun("قطع خودکار با سیگنال ضعیف", "settings", "weak-auto",
                JSONObject().put("enabled", on))
        else
            run("قطع خودکار با سیگنال ضعیف", apRun = { setWeakAuto(on) })

    suspend fun saveLang(lang: String): String =
        if (mode == "vps")
            typedRun("زبان دستگاه", "settings", "language", JSONObject().put("lang", lang))
        else
            run("زبان دستگاه", apRun = { setLang(lang) })

    suspend fun saveSafeZone(on: Boolean, lat: Double, lng: Double, radius: Int): String =
        if (mode == "vps")
            typedRun("محدودهٔ امن", "settings", "safezone", JSONObject()
                .put("enabled", if (on) 1 else 0)
                .put("lat", lat).put("lng", lng).put("radius", radius))
        else
            run("محدودهٔ امن", apRun = { setSafeZone(on, lat, lng, radius) })

    suspend fun saveGps(site: Int, antiCut: Boolean): String =
        if (mode == "vps")
            typedRun("تنظیمات GPS", "settings", "gps",
                JSONObject().put("site", site).put("antiCut", if (antiCut) 1 else 0))
        else
            run("تنظیمات GPS", apRun = { setGpsSite(site, antiCut) })

    /** APN + امنیت قطع آنتن (در AP همراه با شماره‌ها/فرمان‌ها در یک فرم) */
    suspend fun saveSimAll(sec: Boolean, apn: String, phones: List<String>, cmds: List<String>): String {
        return if (mode == "vps") {
            val b = JSONObject().put("enabled", if (sec) 1 else 0)
            if (apn.isNotBlank()) b.put("apn", apn.trim())
            typedRun("تنظیمات SIM800 و امنیت", "settings", "security", b)
        } else {
            run("تنظیمات SIM800 و امنیت", apRun = { setSim800Config(sec, apn, phones, cmds) })
        }
    }

    suspend fun savePhones(phones: List<String>): String =
        if (mode == "vps")
            typedRun("شماره‌های مجاز", "settings", "phones",
                JSONObject().put("list", JSONArray(phones.map { it.trim() })))
        else
            run("شماره‌های مجاز", apRun = {
                post("/setSim800Config", phones.withIndex().map { (i, p) -> "ph$i" to p.trim() })
            })

    suspend fun saveCmds(cmds: List<String>): String =
        if (mode == "vps")
            typedRun("فرمان‌های SMS", "settings", "sms-commands",
                JSONObject().put("cmds", JSONArray(cmds.map { it.trim() })))
        else
            run("فرمان‌های SMS", apRun = {
                post("/setSim800Config", cmds.withIndex().map { (i, c) -> "c$i" to c.trim() })
            })

    suspend fun saveApn(apn: String): String =
        if (mode == "vps")
            typedRun("APN", "settings", "apn", JSONObject().put("value", apn.trim()))
        else
            run("APN", apRun = { post("/setSim800Config", listOf("apn" to apn.trim())) })

    suspend fun detectApn(): String {
        if (mode == "vps") {
            busy = true
            try {
                val r = vps().typed(deviceId, "actions", "apn-detect", JSONObject())
                val cmdId = r.str("cmdId")
                if (r.bool("delivered") && cmdId.isNotEmpty()) {
                    val ack = waitForAck(cmdId, 12)
                    if (ack != null && ack.first) {
                        launch { refresh() }
                        return "APN تشخیص داده شد ✔ (فریمور: ${state.apn})"
                    }
                }
                return "تشخیص APN ارسال شد"
            } catch (e: Exception) {
                return friendly(e)
            } finally {
                busy = false
            }
        }
        return run("تشخیص خودکار APN", apRun = { detectApn() })
    }

    suspend fun rfidReset(): String =
        if (mode == "vps")
            typedRun("پاک‌کردن تگ‌های RFID", "actions", "rfid-reset", JSONObject())
        else
            run("پاک‌کردن تگ‌های RFID", apRun = { rfidReset() })

    /** ثبت تگ جدید — فقط حالت AP (کارت روی خواننده) */
    suspend fun rfidRegister(
        slot: Int, name: String, secret: String, key720: String, uid: String, sha256: String
    ): JSONObject {
        if (mode != "ap") {
            return JSONObject().put("ok", false)
                .put("e", "ثبت تگ فقط به‌صورت محلی ممکن است (اتصال اکسس‌پوینت + کارت روی RC522)")
        }
        return try {
            ap().rfidRegister(slot, name, secret, key720, uid, sha256)
        } catch (e: Exception) {
            JSONObject().put("ok", false).put("e", friendly(e))
        }
    }

    /** تنظیم لینک دستگاه به سرور: حالت AP کامل؛ حالت سرور فقط host/port/interval */
    suspend fun saveVpsLink(
        enabled: Boolean, host: String, port: Int, devId: String, key: String,
        fp: String, interval: Int, test: Boolean
    ): String {
        return if (mode == "ap") {
            run("تنظیمات لینک سرور دستگاه", apRun = {
                setVpsConfig(enabled, host.trim(), port, devId.trim(), key.trim(), fp.trim(), interval, test)
            })
        } else {
            val b = JSONObject().put("interval", interval)
            if (host.isNotBlank()) b.put("host", host.trim())
            if (port in 1..65535) b.put("port", port)
            typedRun("تنظیمات لینک سرور دستگاه", "settings", "server", b)
        }
    }

    /** SSID/رمز/IP اکسس‌پوینت — با تغییر، دستگاه ری‌استارت می‌شود */
    suspend fun saveApCfg(ssid: String, password: String, ip: String): String =
        if (mode == "vps") {
            val b = JSONObject().put("ssid", ssid.trim())
            if (password.isNotEmpty()) b.put("password", password)
            if (ip.isNotEmpty()) b.put("ip", ip.trim())
            typedRun("تنظیمات اکسس‌پوینت", "settings", "ap", b)
        } else {
            run("تنظیمات اکسس‌پوینت (دستگاه ری‌استارت می‌شود)", apRun = {
                apcfg(ssid.trim(), password.ifEmpty { null }, ip.ifEmpty { null })
            })
        }

    // ---------------- حساب/دستگاه‌ها (سرور) ----------------

    suspend fun login(phone: String, pass: String): Boolean {
        return try {
            val r = vps().login(phone.trim(), pass)
            Prefs.token = r.str("token")
            Prefs.phone = phone.trim()
            val u = r.obj("user")
            Prefs.userName = u.str("name")
            isAdmin = u.bool("is_admin")
            loadFromPrefs()
            lastError = ""
            true
        } catch (e: Exception) {
            lastError = friendly(e)
            false
        }
    }

    suspend fun register(phone: String, pass: String, name: String): Boolean {
        return try {
            val r = vps().register(phone.trim(), pass, name.ifEmpty { phone.trim() })
            Prefs.token = r.str("token")
            Prefs.phone = phone.trim()
            val u = r.obj("user")
            Prefs.userName = u.str("name")
            loadFromPrefs()
            lastError = ""
            true
        } catch (e: Exception) {
            lastError = friendly(e)
            false
        }
    }

    suspend fun logout() {
        try { vps().logout() } catch (_: Exception) { }
        Prefs.token = ""
        Prefs.deviceId = ""
        Prefs.deviceName = ""
        loadFromPrefs()
        stopWs()
    }

    suspend fun loadDevices(): Boolean {
        return try {
            devices = vps().devices()
            true
        } catch (e: Exception) {
            lastError = friendly(e)
            false
        }
    }

    suspend fun claim(id: String, key: String, name: String): Boolean {
        return try {
            vps().claimDevice(id.trim(), key.trim(), name.trim())
            loadDevices()
        } catch (e: Exception) {
            lastError = friendly(e)
            false
        }
    }

    suspend fun removeDevice(id: String): Boolean {
        return try {
            vps().removeDevice(id)
            if (deviceId == id) {
                Prefs.deviceId = ""; deviceId = ""
                Prefs.deviceName = ""; deviceName = ""
            }
            loadDevices()
        } catch (e: Exception) {
            lastError = friendly(e)
            false
        }
    }

    fun selectDevice(id: String, name: String) {
        Prefs.deviceId = id
        Prefs.deviceName = name
        deviceId = id
        deviceName = name
        startWs()
        launch { refresh() }
    }
}
