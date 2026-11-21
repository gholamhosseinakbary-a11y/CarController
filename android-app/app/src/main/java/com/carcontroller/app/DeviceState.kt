package com.carcontroller.app

// =====================================================================
// DeviceState.kt — مدل یکسان وضعیت/تنظیمات دستگاه در دو حالت:
//  - حالت VPS : از GET /api/devices/:id/snapshot → lastStat (+cfg)
//  - حالت AP  : از GET /api/info (اندپوینت جدید مرحلهٔ ۴ فریمور)
// =====================================================================

import org.json.JSONObject

data class RfidSlot(
    val slot: Int = 0,
    val name: String = "",
    val uid: String = "",
    val valid: Boolean = false
)

data class DeviceState(
    // وضعیت عمومی
    var upSec: Long = 0,
    var online: Boolean = true,          // سرور: دستگاه آنلاین | AP: دستگاه پاسخ می‌دهد
    var fw: String = "2.0",
    // خروجی‌ها
    var outputs: List<Boolean> = List(4) { false },
    var names: List<String> = List(4) { "" },
    // قفل / امنیت
    var locked: Boolean = false,
    var secLocked: Boolean = false,
    var alarm: Boolean = false,
    var secState: Int = 0,
    var secEnabled: Boolean = false,
    // GSM
    var gsmPct: Int = 0,
    var gsmCsq: Int = 0,
    var gsmReg: Boolean = false,
    var gsmAtt: Boolean = false,
    var carrier: String = "",
    var simIp: String = "",
    var apn: String = "",
    var gprsRx: String = "0.00 KB",     // فقط حالت AP (از /api/info)
    var gprsTx: String = "0.00 KB",     // فقط حالت AP (از /api/info)
    // GPS + محدودهٔ امن
    var gpsFix: Boolean = false,
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var sats: Int = 0,
    var hdop: Double = 0.0,
    var gpsSite: Int = 0,
    var gpsAntiCut: Boolean = false,
    var safeEnabled: Boolean = false,
    var safeLat: Double = 0.0,
    var safeLng: Double = 0.0,
    var safeRadius: Int = 100,
    var safeAlarm: Boolean = false,
    var safeViolated: Boolean = false,
    // تنظیمات AP + سیگنال
    var apSsid: String = "",
    var apPwd: String = "",              // فقط در حالت AP (دستگاه رمز را به سرور نمی‌دهد)
    var apIp: String = "",
    var lang: String = "fa",
    var sigThresh: Int = 50,
    var weakAuto: Boolean = false,
    var rssiPct: Int = 0,
    // شماره‌ها / فرمان‌های SMS
    var phones: List<String> = emptyList(),
    var cmds: List<String> = emptyList(),
    // RFID
    var rfidConfigured: Boolean = false,
    var rfidSlots: List<RfidSlot> = emptyList(),
    // [STEP2-5s] وضعیت پین‌های مرحلهٔ ۲ — p41=پالس استارت فعال (حداکثر ۵ ثانیه)،
    // p42=ورودی تأیید ماژول فرمان‌گیر، st2=تأیید مرحلهٔ ۲ (GPIO42 دیده شد HIGH → آیکون سبز)
    var rfidP41: Boolean = false,
    var rfidP42: Boolean = false,
    var rfidSt2Ok: Boolean = false,
    // لینک VPS دستگاه (فقط حالت AP — از /api/info)
    var vpsEnabled: Boolean = false,
    var vpsStateName: String = "",
    var vpsHost: String = "",
    var vpsPort: Int = 8883,
    var vpsDevId: String = "",
    var vpsKey: String = "",
    var vpsFp: String = "",
    var vpsInterval: Int = 0,
    var vpsErr: String = ""
)

object DeviceStateParse {

    /** حالت AP — خروجی /api/info فریمور (مرحلهٔ ۴) */
    fun fromApInfo(r: JSONObject): DeviceState {
        val s = DeviceState()
        s.upSec = r.lng("up")
        r.obj("ap")?.let {
            s.apSsid = it.str("ssid")
            s.apPwd = it.str("pwd")
            s.apIp = it.str("ip")
        }
        s.lang = r.str("lang", "fa")
        r.obj("sig")?.let {
            s.sigThresh = it.int("thresh", 50)
            s.weakAuto = it.bool("weak")
            s.rssiPct = it.int("pct")
        }
        r.arr("out")?.bools().let { if (it != null && it.size >= 4) s.outputs = it.subList(0, 4) }
        r.arr("names")?.strs().let { if (it != null && it.size >= 4) s.names = it.subList(0, 4) }

        s.locked = r.bool("locked")
        s.secLocked = r.bool("secLocked")
        s.alarm = r.bool("alarm")
        s.secState = r.int("secState")
        s.secEnabled = r.bool("secEn")

        r.obj("gsm")?.let {
            s.gsmPct = it.int("pct")
            s.gsmCsq = it.int("csq")
            s.gsmReg = it.bool("reg")
            s.gsmAtt = it.bool("att")
            s.carrier = it.str("carrier")
            s.simIp = it.str("ip")
            s.apn = it.str("apn")
            if (it.has("rx")) s.gprsRx = String.format(java.util.Locale.US, "%.2f", it.dbl("rx")) + " KB"
            if (it.has("tx")) s.gprsTx = String.format(java.util.Locale.US, "%.2f", it.dbl("tx")) + " KB"
        }
        r.arr("phones")?.strs().let { if (it != null) s.phones = it }
        r.arr("cmds")?.strs().let { if (it != null) s.cmds = it }

        r.obj("gps")?.let {
            s.gpsSite = it.int("site")
            s.gpsAntiCut = it.bool("antiCut")
            s.gpsFix = it.bool("fix")
            s.lat = it.dbl("lat")
            s.lng = it.dbl("lng")
            s.sats = it.int("sats")
            s.hdop = it.dbl("hdop")
        }
        r.obj("safe")?.let {
            s.safeEnabled = it.bool("en")
            s.safeLat = it.dbl("lat")
            s.safeLng = it.dbl("lng")
            s.safeRadius = it.int("rad", 100)
            s.safeAlarm = it.bool("alarm")
            s.safeViolated = it.bool("violated")
        }
        r.obj("rfid")?.let {
            s.rfidConfigured = it.bool("configured")
            s.rfidP41 = it.int("p41") == 1 || it.bool("p41")
            s.rfidP42 = it.int("p42") == 1 || it.bool("p42")
            s.rfidSt2Ok = it.int("st2") == 1 || it.bool("st2")
            val slots = ArrayList<RfidSlot>()
            it.arr("slots")?.objs()?.forEach { t ->
                slots.add(RfidSlot(t.int("s"), t.str("n"), t.str("u"), t.int("v") == 1))
            }
            s.rfidSlots = slots
        }
        r.obj("vps")?.let {
            s.vpsEnabled = it.bool("enabled")
            s.vpsStateName = it.str("stateName")
            s.vpsHost = it.str("host")
            s.vpsPort = it.int("port", 8883)
            s.vpsDevId = it.str("devid")
            s.vpsKey = it.str("key")
            s.vpsFp = it.str("fp")
            s.vpsInterval = it.int("iv")
            s.vpsErr = it.str("err")
        }
        s.online = true
        return s
    }

    /** حالت VPS — خروجی /api/devices/:id/snapshot → lastStat (شامل cfg اختیاری) */
    fun fromVpsSnapshot(st: JSONObject): DeviceState {
        val s = DeviceState()
        s.upSec = st.lng("up")
        s.fw = st.str("fw", "2.0")
        st.arr("out")?.bools().let { if (it != null && it.size >= 4) s.outputs = it.subList(0, 4) }
        st.arr("names")?.strs().let { if (it != null && it.size >= 4) s.names = it.subList(0, 4) }

        s.locked = st.bool("locked")
        s.secLocked = st.bool("secLocked")
        s.alarm = st.bool("alarm")
        s.secState = st.int("secState")
        s.secEnabled = st.bool("secEn")
        s.safeAlarm = st.bool("szAlarm")
        s.safeViolated = st.bool("szViolated")

        st.obj("gsm")?.let {
            s.gsmPct = it.int("pct")
            s.gsmCsq = it.int("csq")
            s.gsmReg = it.bool("reg")
            s.gsmAtt = it.bool("att")
            s.carrier = it.str("car")
            s.simIp = it.str("ip")
        }
        st.obj("gps")?.let {
            s.gpsFix = it.bool("fix")
            s.lat = it.dbl("lat")
            s.lng = it.dbl("lng")
            s.sats = it.int("sats")
            s.hdop = it.dbl("hdop")
        }

        // [STEP2-5s] وضعیت پین‌های RFID از stat سرور (rfid {p41,p42,st2})
        st.obj("rfid")?.let {
            s.rfidP41 = it.int("p41") == 1 || it.bool("p41")
            s.rfidP42 = it.int("p42") == 1 || it.bool("p42")
            s.rfidSt2Ok = it.int("st2") == 1 || it.bool("st2")
        }

        st.obj("cfg")?.let { c ->
            s.sigThresh = c.int("sig", 50)
            s.weakAuto = c.bool("weak")
            s.lang = c.str("lang", "fa")
            s.apn = c.str("apn")
            s.secEnabled = c.bool("sec")
            c.obj("safe")?.let {
                s.safeEnabled = it.bool("en")
                s.safeLat = it.dbl("lat")
                s.safeLng = it.dbl("lng")
                s.safeRadius = it.int("rad", 100)
            }
            c.obj("gps")?.let {
                s.gpsSite = it.int("site")
                s.gpsAntiCut = it.bool("antiCut")
            }
            c.arr("phones")?.strs().let { if (it != null) s.phones = it }
            c.arr("cmds")?.strs().let { if (it != null) s.cmds = it }
            c.obj("ap")?.let {
                s.apSsid = it.str("ssid")
                s.apIp = it.str("ip")
                // رمز AP عمداً به سرور نمی‌رسد
            }
            c.obj("rfid")?.let {
                s.rfidConfigured = it.bool("configured")
                val slots = ArrayList<RfidSlot>()
                it.arr("slots")?.objs()?.forEach { t ->
                    slots.add(RfidSlot(t.int("s"), t.str("n"), t.str("u"), t.int("v") == 1))
                }
                s.rfidSlots = slots
            }
        }
        return s
    }
}
