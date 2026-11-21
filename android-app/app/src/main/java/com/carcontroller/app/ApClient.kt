package com.carcontroller.app

// =====================================================================
// ApClient.kt — کلاینت مستقیم اکسس‌پوینت ESP32-S3 (HTTP محلی)
// ---------------------------------------------------------------------
// اندپوینت‌های JSON فریمور (مرحلهٔ ۴) — بدون اینترنت، فقط وای‌فای دستگاه.
// محدودیت زمانی: ماژول ESP32 نزدیک باید باشد (شعاع وای‌فای).
// =====================================================================

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApClient(val host: String) {

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun get(path: String, query: String = ""): JSONObject =
        call("GET", path + query, null)

    suspend fun post(path: String, form: List<Pair<String, String>>): JSONObject =
        call("POST", path, form)

    private suspend fun call(method: String, path: String, form: List<Pair<String, String>>?): JSONObject =
        withContext(Dispatchers.IO) {
            val url = host.trimEnd('/') + path
            val rb = Request.Builder().url(url)
            val req = when (method) {
                "GET" -> rb.get().build()
                else -> {
                    val fb = FormBody.Builder()
                    if (form != null) for ((k, v) in form) fb.add(k, v)
                    rb.post(fb.build()).build()
                }
            }
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string() ?: ""
                val obj = parseObj(text)
                    ?: JSONObject().put("ok", false).put("e", "پاسخ نامعتبر دستگاه")
                if (!resp.isSuccessful) {
                    throw ApiException(resp.code, obj.str("e", obj.str("message")))
                }
                obj
            }
        }

    // ---------------- snapshot / وضعیت ----------------
    suspend fun info(): JSONObject = get("/api/info")
    suspend fun status(): JSONObject = get("/status")
    suspend fun rssi(): JSONObject = get("/rssi")

    // ---------------- فعال‌سازی ----------------
    suspend fun authorize(code: String): JSONObject = post("/api/authorize", listOf("code" to code.trim()))

    // ---------------- کنترل‌ها ----------------
    suspend fun toggle(pin: Int): JSONObject = get("/toggle", "?pin=$pin")
    suspend fun lock(): JSONObject = post("/api/lock", emptyList())
    suspend fun unlock(): JSONObject = post("/api/unlock", emptyList())
    suspend fun alarm(on: Boolean): JSONObject = post("/api/alarm", listOf("on" to if (on) "1" else "0"))
    suspend fun reboot(): JSONObject = post("/api/reboot", emptyList())

    // ---------------- RFID ----------------
    /** s=1 : مثل نگه‌داشتن کارت ۲ ثانیه (رفع قفل + باز شدن درب‌ها)
     *  s=2 : مثل ارائهٔ دوم کارت ۵ ثانیه (پالس GPIO41 = استارت) */
    suspend fun rfidSim(stage: Int): JSONObject = post("/api/rfid/sim", listOf("s" to stage.toString()))
    suspend fun rfidList(): JSONObject = get("/api/rfid/list")
    suspend fun rfidReset(): JSONObject = post("/api/rfid/reset", emptyList())

    /** ثبت تگ فیزیکی — کارت باید حین ثبت روی RC522 باشد (فقط محلی) */
    suspend fun rfidRegister(
        slot: Int, name: String, secret: String, key720: String, uid: String, sha256: String
    ): JSONObject = post(
        "/rfid/save",
        listOf(
            "slot" to slot.toString(),
            "name" to name.trim(),
            "secret" to secret.trim(),
            "key720" to key720.trim(),
            "uid" to uid.trim(),
            "sha256" to sha256.trim()
        )
    )

    // ---------------- تنظیمات ----------------
    suspend fun setOutputName(pin: Int, name: String): JSONObject =
        post("/setOutputName", listOf("pin" to pin.toString(), "name" to name.trim()))

    suspend fun setSignal(value: Int): JSONObject =
        post("/setSignalThreshold", listOf("value" to value.toString()))

    suspend fun setWeakAuto(enabled: Boolean): JSONObject =
        post("/setWeakAutoDisconnect", listOf("enabled" to if (enabled) "1" else "0"))

    suspend fun setLang(lang: String): JSONObject =
        post("/api/lang", listOf("lang" to lang))

    suspend fun detectApn(): JSONObject = get("/sim800DetectAPN")

    suspend fun setGpsSite(site: Int, antiCut: Boolean): JSONObject =
        post("/setGpsConfig", listOf(
            "site" to site.toString(),
            "gpsAntiCut" to if (antiCut) "1" else "0"
        ))

    suspend fun setSafeZone(enabled: Boolean, lat: Double, lng: Double, radius: Int): JSONObject =
        post("/setGpsConfig", listOf(
            "safeEnabled" to if (enabled) "1" else "0",
            "safeLat" to lat.toString(),
            "safeLng" to lng.toString(),
            "safeRadius" to radius.toString()
        ))

    /** ذخیرهٔ هم‌زمان APN + امنیت + شماره‌ها + فرمان‌های SMS (مثل فرم پنل قبلی) */
    suspend fun setSim800Config(
        sec: Boolean, apn: String, phones: List<String>, cmds: List<String>
    ): JSONObject {
        val form = ArrayList<Pair<String, String>>()
        form.add("sec" to if (sec) "1" else "0")
        form.add("apn" to apn.trim())
        phones.forEachIndexed { i, p -> form.add("ph$i" to p.trim()) }
        cmds.forEachIndexed { i, c -> form.add("c$i" to c.trim()) }
        return post("/setSim800Config", form)
    }

    /** تنظیم لینک سرور دستگاه (enabled/host/port/devid/key/fp/interval + تست) */
    suspend fun setVpsConfig(
        enabled: Boolean?, host: String?, port: Int?, devid: String?, key: String?,
        fp: String?, interval: Int?, test: Boolean = false
    ): JSONObject {
        val form = ArrayList<Pair<String, String>>()
        if (enabled != null) form.add("enabled" to if (enabled) "1" else "0")
        if (host != null) form.add("host" to host.trim())
        if (port != null) form.add("port" to port.toString())
        if (devid != null) form.add("devid" to devid.trim())
        if (key != null) form.add("key" to key.trim())
        if (fp != null) form.add("fp" to fp.trim())
        if (interval != null) form.add("interval" to interval.toString())
        if (test) form.add("test" to "1")
        return post("/setVpsConfig", form)
    }

    /** SSID/رمز/IP اکسس‌پوینت — پس از موفقیت دستگاه ری‌استارت می‌شود */
    suspend fun apcfg(ssid: String?, password: String?, ip: String?): JSONObject {
        val form = ArrayList<Pair<String, String>>()
        if (ssid != null) form.add("ssid" to ssid.trim())
        if (password != null) form.add("password" to password)
        if (ip != null && ip.isNotBlank()) form.add("ip" to ip.trim())
        return post("/api/apcfg", form)
    }
}
