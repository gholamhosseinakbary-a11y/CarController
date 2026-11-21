package com.carcontroller.app.ui.screens

// =====================================================================
// Setup.kt — انتخاب روش اتصال / ورود سرور / فعال‌سازی دستگاه
// ---------------------------------------------------------------------
// صفحهٔ فعال‌سازی (ApConnectScreen) نسخهٔ ۱:۱ صفحهٔ /authorize پنل وب است:
// کارت سفید + عنوان قفل + فرم کد + هشدار زرد.
// صفحات ورود/دستگاه‌ها مخصوص حالت سرور هستند و با همان زبان طراحی ساخته شده‌اند.
// =====================================================================

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.carcontroller.app.ApiException
import com.carcontroller.app.AppModel
import com.carcontroller.app.Prefs
import com.carcontroller.app.go
import com.carcontroller.app.bool
import com.carcontroller.app.str
import com.carcontroller.app.ui.BackLink
import com.carcontroller.app.ui.BtnWeb
import com.carcontroller.app.ui.CC
import com.carcontroller.app.ui.FormFieldWeb
import com.carcontroller.app.ui.HintWeb
import com.carcontroller.app.ui.PageH1
import com.carcontroller.app.ui.StatusMsg
import com.carcontroller.app.ui.WarningBox
import com.carcontroller.app.ui.WhiteContainer
import kotlinx.coroutines.launch
import org.json.JSONObject

// ---------------------------------------------------------------
// ۱) انتخاب روش اتصال (اپ-only): AP مستقیم یا سرور VPS
// ---------------------------------------------------------------
@Composable
fun SetupScreen(nav: NavHostController) {
    var mode by remember { mutableStateOf(if (AppModel.mode.isEmpty()) "" else AppModel.mode) }
    var url by remember { mutableStateOf(Prefs.vpsUrl.ifEmpty { "https://" }) }
    var host by remember { mutableStateOf(Prefs.apHost.ifEmpty { "http://192.168.4.1" }) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        WhiteContainer(pad = PaddingValues(25.dp)) {
            PageH1("🚗 CarController")
            Text(
                "روش اتصال به دستگاه را انتخاب کنید",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = CC.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)
            )

            ModeCard("🖥️", "اتصال از طریق سرور (VPS)", "اینترنت + HTTPS؛ از هر جایی؛ رخدادها/پیامک‌ها/مسیر در سرور ثبت می‌شود", mode == "vps") { mode = "vps" }
            ModeCard("📡", "اتصال مستقیم اکسس‌پوینت", "وای‌فای ESP32-S3 نزدیک خودرو؛ بدون اینترنت و بدون سرور", mode == "ap") { mode = "ap" }

            if (mode == "vps") {
                Spacer(Modifier.height(14.dp))
                Text("نشانی سرور", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = CC.text)
                Spacer(Modifier.height(8.dp))
                FormFieldWeb("https://IP-سرور:8443", url, { url = it }, mono = true)
                HintWeb("همان VPS مرحلهٔ ۱ است (پورت داشبورد HTTPS). مثال: https://185.12.34.56:8443")
                BtnWeb("ذخیره و ورود") {
                    val u = url.trim()
                    if (u.startsWith("http")) {
                        Prefs.vpsUrl = u
                        Prefs.mode = "vps"
                        AppModel.mode = "vps"
                        nav.go("login")
                    }
                }
            }

            if (mode == "ap") {
                Spacer(Modifier.height(14.dp))
                Text("نشانی دستگاه", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = CC.text)
                Spacer(Modifier.height(8.dp))
                FormFieldWeb("http://192.168.4.1", host, { host = it }, mono = true)
                HintWeb("پیش‌فرض ESP32: http://192.168.4.1 — اگر IP اکسس‌پوینت را عوض کرده‌اید همین‌جا وارد کنید.")
                BtnWeb("ادامه") {
                    val h = host.trim()
                    if (h.startsWith("http")) {
                        Prefs.apHost = h
                        Prefs.mode = "ap"
                        AppModel.mode = "ap"
                        nav.go("apconnect")
                    }
                }
            }
        }
    }
}

/** کارت انتخاب روش اتصال — با ظاهر menu-item پنل وب */
@Composable
private fun ModeCard(emoji: String, title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .background(if (selected) CC.infoBg else CC.cardBg, RoundedCornerShape(15.dp))
            .border(
                2.dp,
                if (selected) CC.primary else CC.inputBorder,
                RoundedCornerShape(15.dp)
            )
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Text(emoji, fontSize = 26.sp)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = CC.text)
            Spacer(Modifier.height(4.dp))
            Text(desc, fontSize = 12.sp, color = CC.label, lineHeight = 18.sp)
        }
    }
}

// ---------------------------------------------------------------
// ۲) ورود/ثبت‌نام سرور
// ---------------------------------------------------------------
@Composable
fun LoginScreen(nav: NavHostController) {
    var phone by remember { mutableStateOf(Prefs.phone) }
    var pass by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var reg by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        WhiteContainer(pad = PaddingValues(25.dp)) {
            PageH1("🖥️ ورود به سرور")
            HintWeb(Prefs.vpsUrl)
            FormFieldWeb("شماره موبایل", phone, { phone = it }, mono = true)
            FormFieldWeb("رمز عبور", pass, { pass = it }, secret = true)
            if (reg) FormFieldWeb("نام نمایشی (اختیاری)", name, { name = it })
            BtnWeb(
                if (reg) "ثبت‌نام و ورود" else "ورود",
                enabled = !busy && phone.trim().length >= 7 && pass.length >= 4
            ) {
                busy = true
                scope.launch {
                    val ok = if (reg) AppModel.register(phone, pass, name)
                             else AppModel.login(phone, pass)
                    busy = false
                    if (ok) nav.go(if (Prefs.deviceId.isEmpty()) "devices" else "home")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                androidx.compose.material3.TextButton(onClick = { reg = !reg }) {
                    Text(
                        if (reg) "حساب دارم — ورود" else "حساب ندارم — ثبت‌نام",
                        color = CC.primary, fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!busy && AppModel.lastError.isNotEmpty()) {
                StatusMsg(AppModel.lastError, false)
            }
            BackLink("تغییر روش اتصال") { nav.go("setup") }
        }
    }
}

// ---------------------------------------------------------------
// ۳) فعال‌سازی دستگاه — ۱:۱ با صفحهٔ /authorize پنل وب
// ---------------------------------------------------------------
@Composable
fun ApConnectScreen(nav: NavHostController) {
    var phase by remember { mutableStateOf("check") }     // check | code | ok | none
    var msg by remember { mutableStateOf("در حال بررسی اتصال…") }
    var code by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    fun check() {
        scope.launch {
            msg = "در حال بررسی اتصال…"
            try {
                AppModel.ap().info()
                msg = "دستگاه در دسترس و مجاز است ✔"
                phase = "ok"
            } catch (e: ApiException) {
                if (e.code == 403) {
                    phase = "code"
                    msg = "این گوشی هنوز در دستگاه فعال‌سازی نشده — کد فعال‌سازی را وارد کنید"
                } else {
                    phase = "none"
                    msg = e.message ?: "خطا"
                }
            } catch (e: Exception) {
                phase = "none"
                msg = "دستگاه در دسترس نیست — ابتدا به وای‌فای خودرو متصل شوید"
            }
        }
    }

    LaunchedEffect(Unit) { check() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        WhiteContainer(pad = PaddingValues(25.dp)) {
            PageH1("🔒 فعال‌سازی دستگاه")

            // همان info-text پنل وب
            Text(
                "این دستگاه مجاز نیست. برای ثبت نشانی مک و نشانی آی‌پی، کد فعال‌سازی را وارد کنید.",
                fontSize = 15.sp,
                color = CC.label,
                lineHeight = 26.sp,
                modifier = Modifier.padding(bottom = 15.dp)
            )
            Text(
                "نشانی مک و آی‌پی گوشی شما به‌صورت خودکار از طریق همین اتصال وای‌فای ثبت می‌شود.",
                fontSize = 14.sp,
                color = CC.labelDark,
                modifier = Modifier.padding(bottom = 15.dp)
            )

            // وضعیت اتصال فعلی
            StatusMsg(
                when (phase) {
                    "ok" -> "دستگاه در دسترس و مجاز است ✔"
                    "code" -> "در انتظار کد فعال‌سازی…"
                    else -> msg
                },
                when (phase) { "ok" -> true; "code" -> null; else -> false }
            )

            if (phase == "code") {
                Spacer(Modifier.height(10.dp))
                FormFieldWeb("کد فعال‌سازی", code, { code = it })
                BtnWeb("✅ فعال‌سازی دستگاه") {
                    scope.launch {
                        try {
                            val r = AppModel.ap().authorize(code)
                            if (r.bool("ok") || r.bool("success")) {
                                msg = "دستگاه با موفقیت فعال شد!"
                                check()
                            } else {
                                msg = r.str("e", "کد فعال‌سازی اشتباه است.")
                            }
                        } catch (e: ApiException) {
                            msg = if (e.code == 403) "کد فعال‌سازی اشتباه است." else (e.message ?: "خطا")
                        } catch (e: Exception) {
                            msg = "ارتباط برقرار نشد"
                        }
                    }
                }
            }

            if (phase == "ok") {
                Spacer(Modifier.height(10.dp))
                BtnWeb("🚗 رفتن به صفحه کنترل خودرو", CC.green) { nav.go("home") }
            }

            Spacer(Modifier.height(16.dp))
            WarningBox(
                "⚠️ نکته مهم: اگر گوشی شما از نشانی مک تصادفی استفاده می‌کند، برای این شبکه وای‌فای آن را غیرفعال کنید و نشانی مک واقعی دستگاه را انتخاب کنید. در غیر این صورت ممکن است دوباره نیاز به فعال‌سازی باشد."
            )

            Spacer(Modifier.height(14.dp))
            HintWeb(
                "۱) به وای‌فای دستگاه وصل شوید (SSID پیش‌فرض: CarControl — رمز: 12345678 یا مقادیر شخصی‌سازی‌شده).\n" +
                "۲) اگر گوشی «MAC تصادفی» دارد آن را برای این شبکه خاموش کنید.\n" +
                "۳) دکمهٔ «بررسی اتصال» را بزنید."
            )
            BtnWeb("📶 باز کردن تنظیمات وای‌فای", CC.primary) {
                try { ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) } catch (_: Exception) { }
            }
            BtnWeb("🔄 بررسی اتصال") { check() }
            BackLink("تغییر روش اتصال") { nav.go("setup") }
        }
    }
}

// ---------------------------------------------------------------
// ۴) مدیریت خودروها (حالت سرور)
// ---------------------------------------------------------------
@Composable
fun DevicesScreen(nav: NavHostController) {
    var id by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }
    var msgOk by remember { mutableStateOf<Boolean?>(null) }
    var delConfirm by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { AppModel.loadDevices() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        WhiteContainer(pad = PaddingValues(25.dp)) {
            PageH1("🚗 خودروهای من")

            if (AppModel.devices.isEmpty()) {
                HintWeb("هنوز دستگاهی اضافه نشده — با فرم «افزودن خودرو» (DeviceID و کلید مرحلهٔ ۲) اضافه کنید.")
            }

            AppModel.devices.forEach { d ->
                DeviceRow(d, nav, onDelete = { delConfirm = d.str("id") })
            }

            Text("افزودن خودرو", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = CC.text)
            Spacer(Modifier.height(8.dp))
            FormFieldWeb("شناسهٔ دستگاه (CAR-xxxxxx)", id, { id = it })
            FormFieldWeb("کلید دستگاه (AuthKey — ۳۲ نویسه hex)", key, { key = it })
            FormFieldWeb("نام خودرو (اختیاری)", name, { name = it })
            BtnWeb(
                "افزودن",
                enabled = id.trim().length >= 4 && key.trim().length >= 16
            ) {
                scope.launch {
                    val ok = AppModel.claim(id, key, name)
                    msg = if (ok) "افزوده شد ✔" else AppModel.lastError
                    msgOk = ok
                    if (ok) { id = ""; key = ""; name = "" }
                }
            }
            StatusMsg(msg, msgOk)

            androidx.compose.material3.TextButton(
                onClick = {
                    scope.launch {
                        AppModel.logout()
                        nav.go("login")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("خروج از حساب", color = CC.label, fontWeight = FontWeight.Bold)
            }
            BackLink("تغییر روش اتصال") { nav.go("setup") }
        }
    }

    if (delConfirm != null) {
        com.carcontroller.app.ui.ConfirmWeb(
            title = "حذف خودرو",
            text = "دستگاه ${delConfirm ?: ""} از حساب شما حذف می‌شود؛ ادامه می‌دهید؟",
            onConfirm = {
                scope.launch { AppModel.removeDevice(delConfirm ?: "") }
                delConfirm = null
            },
            onDismiss = { delConfirm = null }
        )
    }
}

@Composable
private fun DeviceRow(d: JSONObject, nav: NavHostController, onDelete: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(CC.cardBg, RoundedCornerShape(15.dp))
            .padding(15.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                d.str("name").ifEmpty { d.str("id") },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = CC.text,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (d.bool("online")) "آنلاین ✔" else "آفلاین",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (d.bool("online")) CC.ok else CC.label
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            BtnWeb("کنترل", CC.primary) {
                AppModel.selectDevice(d.str("id"), d.str("name"))
                nav.go("home")
            }
        }
        Text("ID: ${d.str("id")}", fontSize = 12.sp, color = CC.hintC, modifier = Modifier.padding(top = 4.dp))
        androidx.compose.material3.TextButton(onClick = onDelete) {
            Text("حذف", color = CC.err, fontWeight = FontWeight.Bold)
        }
    }
}
