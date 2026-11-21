package com.carcontroller.app.ui.screens

// =====================================================================
// SettingsMore.kt — نسخهٔ ۱:۱ صفحات SIM800 / GPS / RFID / سرور VPS
// ---------------------------------------------------------------------
// ساختار، متن‌ها، رنگ‌ها و ترتیب دقیقاً همان صفحات وب اکسس‌پوینت است.
// =====================================================================

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.carcontroller.app.AppModel
import com.carcontroller.app.int
import com.carcontroller.app.obj
import com.carcontroller.app.str
import com.carcontroller.app.ui.AlarmBanner
import com.carcontroller.app.ui.BackLink
import com.carcontroller.app.ui.BtnWeb
import com.carcontroller.app.ui.CC
import com.carcontroller.app.ui.ConfirmWeb
import com.carcontroller.app.ui.FieldsetWeb
import com.carcontroller.app.ui.FormFieldWeb
import com.carcontroller.app.ui.HintWeb
import com.carcontroller.app.ui.InfoBox
import com.carcontroller.app.ui.PageH1
import com.carcontroller.app.ui.SectionTitleWeb
import com.carcontroller.app.ui.StatusCard
import com.carcontroller.app.ui.StatusMsg
import com.carcontroller.app.ui.StatusRowWeb
import com.carcontroller.app.ui.SuccessBox
import com.carcontroller.app.ui.SwitchRowWeb
import com.carcontroller.app.ui.WhiteContainer
import kotlinx.coroutines.launch
import java.util.Locale

private fun fmt6(v: Double): String = String.format(Locale.US, "%.6f", v)

/** فاصلهٔ تا مرکز محدودهٔ امن (همان haversine جاوااسکریپت وب) */
private fun haversine(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371000.0
    val p1 = Math.toRadians(lat1); val p2 = Math.toRadians(lat2)
    val dp = Math.toRadians(lat2 - lat1); val dl = Math.toRadians(lng2 - lng1)
    val a = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2)
    return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

// ---------------------------------------------------------------
// 📱 SIM800 — ۱:۱ با /sim800 پنل وب
// ---------------------------------------------------------------
@Composable
fun SimSettingsScreen(nav: NavHostController) {
    val s = AppModel.state
    var apn by remember { mutableStateOf(s.apn) }
    val phones = remember { mutableStateOf(List(5) { s.phones.getOrElse(it) { "" } }) }
    val cmds = remember { mutableStateOf(List(7) { s.cmds.getOrElse(it) { "" } }) }
    var sec by remember { mutableStateOf(s.secEnabled) }
    var msg by remember { mutableStateOf("") }
    var msgOk by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    // مقادیر وضعیت — همان منطق جاوااسکریپت refreshStatus
    val gsmTxt: String; val gsmTone: androidx.compose.ui.graphics.Color
    if (s.gsmPct > 0) {
        gsmTxt = "${s.gsmPct}% (CSQ ${s.gsmCsq})"
        gsmTone = if (s.gsmPct >= 65) CC.ok else if (s.gsmPct >= 30) CC.warn else CC.err
    } else { gsmTxt = "initializing..."; gsmTone = CC.warn }
    val gprsTxt: String; val gprsTone: androidx.compose.ui.graphics.Color
    if (s.simIp.isNotEmpty()) { gprsTxt = "Online"; gprsTone = CC.ok }
    else if (s.gsmAtt && s.gsmReg) { gprsTxt = "Attached (no IP yet)"; gprsTone = CC.warn }
    else if (s.gsmReg) { gprsTxt = "Registered"; gprsTone = CC.warn }
    else { gprsTxt = "No network"; gprsTone = CC.err }
    val secNames = listOf("Normal", "GRACE - buzzer", "User connected", "LOCKED")
    val secTxt = if (!s.secEnabled) "disabled"
    else secNames.getOrElse(s.secState) { "?" }
    val secTone = if (!s.secEnabled) CC.text
    else when (s.secState) { 0 -> CC.ok; 3 -> CC.err; else -> CC.warn }

    SettingsScaffold("", nav, back = false) {
        PageH1("📱 تنظیمات SIM800")

        // ---------- status-card (۸ ردیف — همان ترتیب وب) ----------
        StatusCard {
            StatusRowWeb("📶 GSM Signal", gsmTxt, gsmTone)
            StatusRowWeb("🌐 Internet", gprsTxt, gprsTone)
            StatusRowWeb("📡 Carrier", s.carrier.ifEmpty { "-" })
            StatusRowWeb("🔗 IP", s.simIp.ifEmpty { "-" })
            if (AppModel.mode == "ap") {
                // [UI-v5] دیتای RX/TX از صفحهٔ اصلی (سطر GPRS سابق) به اینجا منتقل شد
                StatusRowWeb("⬇️ Data received", s.gprsRx, CC.text)
                StatusRowWeb("⬆️ Data sent", s.gprsTx, CC.text)
            }
            StatusRowWeb("📞 Call", "idle")
            StatusRowWeb("🛡️ Security", secTxt, secTone)
            StatusRowWeb("🔒 Car lock", if (s.locked) "LOCKED" else "free", if (s.locked) CC.err else CC.ok)
            StatusRowWeb("🔔 Alarm", if (s.alarm) "ON" else "off", if (s.alarm) CC.warn else CC.text, last = true)
        }

        // ---------- APN ----------
        SectionTitleWeb("🔧 APN Configuration")
        FormFieldWeb("APN (خالی = تشخیص خودکار)", apn, { apn = it }, placeholder = "mcinet / mtnirancell / rightel")
        BtnWeb("🔍 تشخیص خودکار APN", CC.orange) {
            scope.launch {
                val r = AppModel.detectApn()
                msg = r
                msgOk = r.contains("✔")
                apn = AppModel.state.apn
            }
        }
        HintWeb("فرمان Location فقط از GPS استفاده می‌کند و به اینترنت نیاز ندارد.")

        // ---------- شماره‌های مجاز ----------
        SectionTitleWeb("📞 شماره‌های مجاز")
        HintWeb("فقط این شماره‌ها می‌توانند فرمان پیامکی بفرستند یا تماس بگیرند.")
        for (i in 0 until 5) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    "${i + 1}.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF999999),
                    modifier = Modifier.width(28.dp)
                )
                FormFieldWeb("", phones.value[i], { phones.value = phones.value.toMutableList().also { l -> l[i] = it } })
            }
        }

        // ---------- فرمان‌های SMS ----------
        SectionTitleWeb("✉️ فرمان‌های پیامکی (SMS)")
        HintWeb("متن پیامی که کاربر می‌فرستد. فقط حروف لاتین، بدون تفاوت حروف بزرگ/کوچک، حداکثر ۳۰ نویسه.")
        val labels = listOf(
            "📍 Location", "🔒 Lock", "🔓 Unlock", "🔔 Alarm ON",
            "🔕 Alarm OFF", "🛑 Stop Location", "📊 Status"
        )
        for (i in 0 until 7) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    labels[i],
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = CC.labelDark,
                    modifier = Modifier.width(110.dp)
                )
                FormFieldWeb("", cmds.value[i], { cmds.value = cmds.value.toMutableList().also { l -> l[i] = it } })
            }
        }
        HintWeb(
            "در فرمان «${cmds.value.getOrElse(0) { "" }}» می‌توانید فاصله‌ی ارسال لوکیشن را با عدد تعیین کنید؛ مثال: «${cmds.value.getOrElse(0) { "" }} 15» یعنی ارسال لوکیشن هر ۱۵ دقیقه (از ۱ تا ۱۲۰ دقیقه). فرمان «${cmds.value.getOrElse(5) { "" }}» ارسال لوکیشن را متوقف می‌کند و فرمان «${cmds.value.getOrElse(6) { "" }}» وضعیت کامل دستگاه را فقط یک‌بار می‌فرستد."
        )

        // ---------- امنیت قطع آنتن ----------
        SectionTitleWeb("🛡️ امنیت قطع آنتن سیم‌کارت")
        SwitchRowWeb(
            "با قطع آنتن سیم‌کارت: ۵ دقیقه بازر، سپس قفل و قطع سوئیچ و پمپ بنزین",
            sec, { sec = it }
        )
        HintWeb("در مهلت ۵ دقیقه اگر دستگاه مجاز به وای‌فای متصل شود یا آنتن برگردد، بازر قطع می‌شود و قفلی اعمال نمی‌شود. در صورت قفل شدن، با برگشت آنتن یا اتصال به وای‌فای، سوئیچ و پمپ آزاد می‌شوند.")

        BtnWeb("💾 ذخیره", CC.green) {
            scope.launch {
                val list = phones.value.map { it.trim() }.filter { it.isNotEmpty() }
                val r = AppModel.saveSimAll(sec, apn, list, cmds.value.map { it.trim() })
                msg = r
                msgOk = !r.contains("✖")
            }
        }
        StatusMsg(msg, msgOk)
        BackLink("← بازگشت به منوی تنظیمات") { nav.popBackStack() }
    }
}

// ---------------------------------------------------------------
// 🛰️ GPS و محدودهٔ امن — ۱:۱ با /gps پنل وب
// ---------------------------------------------------------------
@Composable
fun GpsSettingsScreen(nav: NavHostController) {
    val s = AppModel.state
    var site by remember { mutableStateOf(s.gpsSite) }
    var antiCut by remember { mutableStateOf(s.gpsAntiCut) }
    var safeOn by remember { mutableStateOf(s.safeEnabled) }
    var lat by remember { mutableStateOf(if (s.safeLat != 0.0) fmt6(s.safeLat) else "") }
    var lng by remember { mutableStateOf(if (s.safeLng != 0.0) fmt6(s.safeLng) else "") }
    var rad by remember { mutableStateOf(if (s.safeRadius > 0) s.safeRadius.toString() else "100") }
    var msg by remember { mutableStateOf("") }
    var msgOk by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    val stTxt = if (s.gpsFix) "FIX" else "Searching satellites..."
    val stTone = if (s.gpsFix) CC.ok else CC.warn
    val distTxt = if (s.gpsFix && s.safeEnabled && s.safeLat != 0.0) {
        val d = haversine(s.lat, s.lng, s.safeLat, s.safeLng)
        "${d.toInt()} m (zone ${s.safeRadius}m)"
    } else "-"

    fun mapUrl(): String {
        val la = AppModel.state.lat
        val lo = AppModel.state.lng
        return when (site) {
            1 -> "https://neshan.org/maps@$la,$lo"
            2 -> "https://balad.ir/@$la,$lo"
            else -> "https://maps.google.com/?q=$la,$lo"
        }
    }

    SettingsScaffold("", nav, back = false) {
        PageH1("🛰️ تنظیمات GPS")

        if (s.safeAlarm) AlarmBanner("⚠️ SAFE ZONE ALARM ACTIVE")

        // ---------- status-card (۵ ردیف — همان وب) ----------
        StatusCard {
            StatusRowWeb("Status", stTxt, stTone)
            StatusRowWeb("Satellites", "${s.sats}")
            StatusRowWeb("Latitude", if (s.gpsFix) fmt6(s.lat) else "-")
            StatusRowWeb("Longitude", if (s.gpsFix) fmt6(s.lng) else "-")
            StatusRowWeb("Distance to center", distTxt, last = true)
        }

        // ---------- تشخیص قطع آنتن ----------
        SwitchRowWeb("🛰️ GPS Antenna Cut Detection", antiCut, { antiCut = it })
        HintWeb("در صورت قطع آنتن GPS بیش از ۵ دقیقه، آلارم فعال می‌شود.")

        // ---------- Map Site ----------
        SectionTitleWeb("Map Site")
        Row {
            SiteBtn("Google Maps", 0, site) { site = 0 }
            SiteBtn("نشان (Neshan)", 1, site) { site = 1 }
            SiteBtn("بلد (Balad)", 2, site) { site = 2 }
        }
        HintWeb("سایت نقشه در لینک‌های ارسالی (پیامک لوکیشن) استفاده می‌شود.")
        BtnWeb("🗺️ باز کردن در نقشه", CC.primary) {
            try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapUrl()))) } catch (_: Exception) { }
        }

        // ---------- محدودهٔ امن ----------
        SectionTitleWeb("🔒 محدوده امن")
        SwitchRowWeb("فعال‌سازی محدوده امن", safeOn, { safeOn = it })
        FormFieldWeb("عرض جغرافیایی", lat, { lat = it }, placeholder = "35.689200")
        FormFieldWeb("طول جغرافیایی", lng, { lng = it }, placeholder = "51.389000")
        FormFieldWeb("شعاع (متر)", rad, { rad = it }, numeric = true)
        BtnWeb("📍 استفاده از موقعیت فعلی", CC.orange) {
            if (AppModel.state.gpsFix || AppModel.state.lat != 0.0) {
                lat = fmt6(AppModel.state.lat)
                lng = fmt6(AppModel.state.lng)
            } else {
                msg = "No GPS fix yet - wait for satellites"
                msgOk = false
            }
        }
        HintWeb("اگر خارج از محدوده امن یا قطع سیگنال GPS (بیش از ۶۰ ثانیه) رخ دهد، خودرو قفل و به همه شماره‌های مجاز تماس زده می‌شود و سرور مطلع می‌شود.")

        BtnWeb("💾 Save Settings", CC.green) {
            scope.launch {
                val la = lat.toDoubleOrNull()
                val lo = lng.toDoubleOrNull()
                val r = rad.toIntOrNull()
                msg = if (la == null || lo == null || r == null || r < 1 || r > 100000)
                    "مختصات/شعاع نامعتبر است"
                else AppModel.saveSafeZone(safeOn, la, lo, r)
                msgOk = !msg.contains("نامعتبر") && !msg.contains("✖")
            }
        }
        // ذخیرهٔ تنظیم GPS (سایت نقشه + ضدقطعی) — مثل saveAll وب
        BtnWeb("💾 ذخیرهٔ تنظیمات GPS (سایت/ضدقطعی)", CC.green) {
            scope.launch {
                val r = AppModel.saveGps(site, antiCut)
                msg = r
                msgOk = !r.contains("✖")
            }
        }
        StatusMsg(msg, msgOk)
        BackLink("← بازگشت به منوی تنظیمات") { nav.popBackStack() }
    }
}

/** دکمه‌های سه‌گانهٔ سایت نقشه — سبک select وب */
@Composable
private fun SiteBtn(text: String, value: Int, current: Int, onClick: () -> Unit) {
    val sel = value == current
    Row(
        Modifier
            .padding(end = 8.dp, top = 4.dp, bottom = 4.dp)
            .background(if (sel) CC.primary else CC.cardBg, RoundedCornerShape(8.dp))
            .border(1.dp, if (sel) CC.primary else CC.inputBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (sel) androidx.compose.ui.graphics.Color.White else CC.labelDark
        )
    }
}

// ---------------------------------------------------------------
// 🔑 RFID — ۱:۱ با /rfid پنل وب + عملیات مرحلهٔ ۱/۲ (خواستهٔ مرحلهٔ ۴)
// ---------------------------------------------------------------
@Composable
fun RfidSettingsScreen(nav: NavHostController) {
    val s = AppModel.state
    var resetAsk by remember { mutableStateOf(false) }
    var simAsk by remember { mutableStateOf(0) }
    var msg by remember { mutableStateOf("") }
    var msgOk by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    // فرم تگ جدید — مثل وب: هر جایگاه خالی فرم مستقل خودش را دارد

    SettingsScaffold("", nav, back = false) {
        PageH1("🔑 تنظیمات RFID")

        InfoBox(
            "برای هر تگ، نام دقیق و فقط یکی از کلیدهای secret_b64 یا key720_b64 را وارد کنید. سپس تگ را روی RC522 نگه دارید و «بررسی و ذخیره» را بزنید. پس از ثبت هر دو تگ، این بخش‌ها پنهان و آیتم RFID از منوی تنظیمات مخفی می‌شود."
        )

        // وضعیت هر جایگاه — مثل وب: تگ ثبت‌شده جعبهٔ سبز، جایگاه خالی فرم ثبت
        for (i in 0 until 2) {
            val t = s.rfidSlots.firstOrNull { it.slot == i }
            val valid = t?.valid ?: false
            if (valid) {
                SuccessBox("✓ تگ ${i + 1} با موفقیت ثبت شده است.")
            } else {
                RfidRegisterForm(i) { ok, m ->
                    msg = m
                    msgOk = ok
                }
            }
        }

        // ---------- عملیات (مثل فشار کارت) — دکمه‌های مرحلهٔ ۴ ----------
        if (s.rfidConfigured) {
            SectionTitleWeb("عملیات (مثل فشار کارت)")
            BtnWeb("🔓 مرحلهٔ ۱ — رفع قفل + باز شدن درب‌ها", CC.green) { simAsk = 1 }
            BtnWeb("🚀 مرحلهٔ ۲ — پالس استارت (GPIO41)", CC.orange) { simAsk = 2 }
            HintWeb("مرحلهٔ ۱: مثل نگه‌داشتن کارت ۲ ثانیه — مرحلهٔ ۲: مثل ارائهٔ دوم کارت ۵ ثانیه.")
            BtnWeb("🗑 پاک‌کردن تگ‌ها (RFIDRESET)", CC.red) { resetAsk = true }
            HintWeb("پاک‌کردن برای ثبت تگ‌های جدید لازم است (RFIDRESET).")
        } else {
            HintWeb("پس از پیکربندی کامل هر دو تگ، عملیات مرحلهٔ ۱/۲ از همین صفحه و صفحهٔ اصلی در دسترس است.")
        }

        StatusMsg(msg, msgOk)
        BackLink("← بازگشت به منوی تنظیمات") { nav.popBackStack() }
    }

    if (resetAsk) {
        ConfirmWeb(
            title = "پاک‌کردن تگ‌های RFID",
            text = "هر دو جایگاه تگ خالی می‌شوند؛ برای استفاده مجدد باید تگ‌ها را دوباره ثبت کنید. ادامه؟",
            onConfirm = {
                resetAsk = false
                scope.launch {
                    val r = AppModel.rfidReset()
                    msg = r; msgOk = !r.contains("✖")
                }
            },
            onDismiss = { resetAsk = false }
        )
    }
    if (simAsk > 0) {
        ConfirmWeb(
            title = if (simAsk == 1) "اجرای مرحلهٔ ۱ تگ RFID" else "اجرای مرحلهٔ ۲ تگ RFID",
            text = if (simAsk == 1)
                "مثل ۲ ثانیه نگه‌داشتن کارت: رفع قفل + باز شدن درب‌ها. ادامه؟"
            else
                "مثل ارائهٔ دوم کارت ۵ ثانیه: پالس استارت خودرو. ادامه؟",
            onConfirm = {
                val st = simAsk
                simAsk = 0
                scope.launch {
                    val r = AppModel.rfidStage(st)
                    msg = r; msgOk = !r.contains("✖")
                }
            },
            onDismiss = { simAsk = 0 }
        )
    }
}

/** فرم ثبت تگ یک جایگاه — همان fieldset وب با اعتبارسنجی‌های جاوااسکریپت پنل */
@Composable
private fun RfidRegisterForm(slot: Int, onResult: (Boolean, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var key720 by remember { mutableStateOf("") }
    var uid by remember { mutableStateOf("") }
    var sha256 by remember { mutableStateOf("") }
    var errName by remember { mutableStateOf("") }
    var errSecret by remember { mutableStateOf("") }
    var errKey720 by remember { mutableStateOf("") }
    var errUid by remember { mutableStateOf("") }
    var errSha by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    FieldsetWeb("تگ ${slot + 1}") {
        FormFieldWeb("نام تگ", name, { name = it })
        if (errName.isNotEmpty()) ErrText(errName)
        FormFieldWeb("secret_b64", secret, { secret = it }, multiLine = true, mono = true)
        if (errSecret.isNotEmpty()) ErrText(errSecret)
        FormFieldWeb("یا key720_b64", key720, { key720 = it }, multiLine = true, mono = true)
        if (errKey720.isNotEmpty()) ErrText(errKey720)
        FormFieldWeb("UID (اختیاری)", uid, { uid = it }, mono = true)
        if (errUid.isNotEmpty()) ErrText(errUid)
        FormFieldWeb("SHA256 (اختیاری)", sha256, { sha256 = it }, mono = true)
        if (errSha.isNotEmpty()) ErrText(errSha)
        HintWeb("فقط یکی از secret_b64 یا key720_b64 را وارد کنید. کارت را حین ذخیره روی RC522 نگه دارید — فریمور قبل از ذخیره، AES-GCM کارت را تأیید می‌کند.")
        BtnWeb("بررسی تگ و ذخیره") {
            // همان اعتبارسنجی جاوااسکریپت پنل وب
            var invalid = false
            errName = ""; errSecret = ""; errKey720 = ""; errUid = ""; errSha = ""
            if (name.isBlank() || name.length > 100) { errName = "نام تگ باید بین ۱ تا ۱۰۰ نویسه باشد."; invalid = true }
            if (secret.isBlank() && key720.isBlank()) { errSecret = "یکی از دو کلید را وارد کنید."; invalid = true }
            if (secret.isNotBlank() && key720.isNotBlank()) { errKey720 = "فقط یکی از دو کلید را وارد کنید."; invalid = true }
            val uidRe = Regex("^[a-fA-F0-9]{8,20}$")
            if (uid.isNotBlank() && !uidRe.matches(uid)) { errUid = "UID باید ۸ تا ۲۰ نویسه هگزادسیمال باشد."; invalid = true }
            val shaRe = Regex("^[a-fA-F0-9]{64}$")
            if (sha256.isNotBlank() && !shaRe.matches(sha256)) { errSha = "SHA256 باید ۶۴ نویسه هگزادسیمال باشد."; invalid = true }
            if (invalid) return@BtnWeb
            scope.launch {
                val r = AppModel.rfidRegister(slot, name, secret, key720, uid, sha256)
                val ok = r.optBoolean("ok")
                onResult(ok, if (ok) "تگ با موفقیت بررسی و ذخیره شد."
                        else r.str("message", r.str("e", "خطا در ذخیره‌سازی.")))
            }
        }
    }
}

@Composable
private fun ErrText(msg: String) {
    Text(
        msg,
        fontSize = 13.sp,
        color = Color(0xFFFF8A80),
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

// ---------------------------------------------------------------
// 🖥️ سرور VPS — ۱:۱ با /vps پنل وب
// ---------------------------------------------------------------
@Composable
fun VpsSettingsScreen(nav: NavHostController) {
    val s = AppModel.state
    var msg by remember { mutableStateOf("") }
    var msgOk by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    val stateNames = mapOf(
        "OFF" to "خاموش", "WAIT_NET" to "در انتظار شبکه", "BACKOFF" to "انتظار برای تلاش مجدد",
        "BRINGUP" to "برقراری GPRS", "TLS" to "دستی دادن TLS", "AUTH" to "احراز هویت",
        "ONLINE" to "متصل ✔"
    )

    if (AppModel.mode == "ap") {
        var enabled by remember { mutableStateOf(s.vpsEnabled) }
        var host by remember { mutableStateOf(s.vpsHost) }
        var port by remember { mutableStateOf(if (s.vpsPort > 0) s.vpsPort.toString() else "8883") }
        var devid by remember { mutableStateOf(s.vpsDevId) }
        var key by remember { mutableStateOf(s.vpsKey) }
        var fp by remember { mutableStateOf(s.vpsFp) }
        var interval by remember { mutableStateOf(if (s.vpsInterval > 0) s.vpsInterval.toString() else "0") }

        SettingsScaffold("", nav, back = false) {
            PageH1("🖥️ تنظیمات سرور (VPS)")

            // ---------- status-card (۶ ردیف — همان وب) ----------
            StatusCard {
                StatusRowWeb("وضعیت لینک", stateNames[s.vpsStateName] ?: s.vpsStateName.ifEmpty { "خاموش" },
                    if (s.vpsStateName == "ONLINE") CC.ok else CC.warn)
                StatusRowWeb("آخرین خطا", s.vpsErr.ifEmpty { "-" }, if (s.vpsErr.isNotEmpty()) CC.err else CC.text)
                StatusRowWeb("فعال", if (enabled) "بله" else "خیر")
                StatusRowWeb("شناسه دستگاه", s.vpsDevId.ifEmpty { "-" })
                StatusRowWeb("آدرس سرور", s.vpsHost.ifEmpty { "-" })
                StatusRowWeb("بازه وضعیت فعلی", "${s.vpsInterval} ثانیه", last = true)
            }

            SwitchRowWeb("🔌 اتصال به سرور فعال باشد", enabled, { enabled = it })

            FormFieldWeb("آدرس سرور (IP یا دامنه)", host, { host = it }, placeholder = "vps.example.com یا 185.x.x.x")
            FormFieldWeb("پورت Gateway", port, { port = it }, numeric = true, placeholder = "8883")
            FormFieldWeb("شناسه دستگاه (DeviceID)", devid, { devid = it })
            HintWeb("همین مقدار را در داشبورد سرور در فرم «افزودن خودرو» وارد کنید. با فرمان سریالی «VPS key» هم قابل مشاهده است.")
            FormFieldWeb("کلید دستگاه (AuthKey)", key, { key = it })
            HintWeb("کلید ۳۲ نویسه‌ای هگز — مثل رمز عبور دستگاه در برابر سرور؛ فقط داخل تونل TLS ارسال می‌شود.")
            FormFieldWeb("اثر انگشت گواهی سرور (SHA-256، اختیاری)", fp, { fp = it }, mono = true,
                placeholder = "خالی = قفل خودکار در اولین اتصال (TOFU)")
            HintWeb("اگر خالی بگذارید، در اولین اتصال اثر انگشت سرور قفل می‌شود (مثل SSH) و اتصال‌های بعدی با آن مقایسه می‌شود. برای مقدار اولیه، خروجی اسکریپت gen-cert.sh روی سرور را کپی کنید.")
            FormFieldWeb("بازه ارسال وضعیت (ثانیه)", interval, { interval = it }, numeric = true,
                placeholder = "0 = تنظیم سرور (60)")
            HintWeb("۰ تا ۳۶۰۰؛ عدد کوچک‌تر = بروزتر ولی مصرف داده GPRS بیشتر. مقدار 0 یعنی از تنظیم سرور پیروی کن.")

            BtnWeb("💾 ذخیره تنظیمات", CC.green) {
                scope.launch {
                    val r = AppModel.saveVpsLink(
                        enabled, host, port.toIntOrNull() ?: 8883,
                        devid, key, fp, interval.toIntOrNull() ?: 0, false
                    )
                    msg = r; msgOk = !r.contains("✖")
                }
            }
            BtnWeb("🔌 تست / اتصال فوری", CC.orange) {
                scope.launch {
                    val r = AppModel.saveVpsLink(
                        enabled, host, port.toIntOrNull() ?: 8883,
                        devid, key, fp, interval.toIntOrNull() ?: 0, true
                    )
                    msg = r; msgOk = !r.contains("✖")
                }
            }
            StatusMsg(msg, msgOk)
            BackLink("← بازگشت به منوی تنظیمات") { nav.popBackStack() }
        }
    } else {
        // حالت سرور: فقط بازهٔ ارسال وضعیت (سیاست امنیتی — هاست/کلید در دسترس نیست)
        var interval by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            try {
                val r = AppModel.vps().snapshot(AppModel.deviceId)
                val dev = r.obj("device")
                val iv = dev?.int("stat_interval", 0) ?: 0
                if (iv > 0) interval = iv.toString()
            } catch (_: Exception) { }
        }
        SettingsScaffold("", nav, back = false) {
            PageH1("🖥️ تنظیمات سرور (VPS)")
            StatusCard {
                StatusRowWeb("خودرو", AppModel.deviceName.ifEmpty { AppModel.deviceId })
                StatusRowWeb("سرور", if (AppModel.connected) "متصل ✔" else "قطع",
                    if (AppModel.connected) CC.ok else CC.err)
                StatusRowWeb("وضعیت لینک", stateNames[s.vpsStateName] ?: (s.vpsStateName.ifEmpty { "—" }),
                    if (s.vpsStateName == "ONLINE") CC.ok else CC.warn)
                StatusRowWeb("بازه وضعیت فعلی", if (s.vpsInterval > 0) "${s.vpsInterval} ثانیه" else "از سرور", last = true)
            }
            HintWeb("تنظیمات هاست/پورت/کلید دستگاه فقط در حالت «اتصال مستقیم اکسس‌پوینت» قابل مشاهده/ویرایش است (ایمنی). اینجا فقط بازهٔ ارسال وضعیت از سرور قابل تغییر است.")
            FormFieldWeb("بازه ارسال وضعیت (ثانیه؛ ۰=پیش‌فرض ۶۰)", interval, { interval = it }, numeric = true)
            BtnWeb("💾 ذخیرهٔ بازهٔ وضعیت", CC.green) {
                scope.launch {
                    val iv = interval.toIntOrNull() ?: 0
                    msg = if (iv != 0 && (iv < 10 || iv > 3600)) "بازه باید ۱۰ تا ۳۶۰۰ یا ۰ باشد"
                    else AppModel.saveVpsLink(true, "", 0, "", "", "", iv, false)
                    msgOk = !msg.contains("بازه باید") && !msg.contains("✖")
                }
            }
            StatusMsg(msg, msgOk)
            BackLink("← بازگشت به منوی تنظیمات") { nav.popBackStack() }
        }
    }
}
