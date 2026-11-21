package com.carcontroller.app.ui.screens

// =====================================================================
// Settings.kt — نسخهٔ ۱:۱ صفحات «منوی تنظیمات» و «تنظیمات نقطه دسترسی»
// ---------------------------------------------------------------------
// منوی تنظیمات: ردیف‌های پنل وب (اکسس‌پوینت/SIM800/RFID/GPS/VPS)
// + [RFID-HIDE] پس از ثبت هر دو تگ (rfidConfigured)، آیتم RFID از منو
//   مخفی می‌شود و فقط با پاک‌کردن تگ‌ها دوباره ظاهر می‌شود.
// تنظیمات نقطه دسترسی: فرم SSID/رمز/IP + هشدار + جعبهٔ آستانهٔ سیگنال.
// =====================================================================

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import com.carcontroller.app.AppModel
import com.carcontroller.app.ui.BackLink
import com.carcontroller.app.ui.BtnWeb
import com.carcontroller.app.ui.CC
import com.carcontroller.app.ui.ConfirmWeb
import com.carcontroller.app.ui.FormFieldWeb
import com.carcontroller.app.ui.MenuItemWeb
import com.carcontroller.app.ui.PageH1
import com.carcontroller.app.ui.StatusMsg
import com.carcontroller.app.ui.WarningBox
import com.carcontroller.app.ui.WhiteContainer
import kotlinx.coroutines.launch

/** قاب مشترک صفحات تنظیمات — کانتینر سفید روی گرادیان + لینک بازگشت پایین */
@Composable
fun SettingsScaffold(
    title: String,
    nav: NavHostController,
    back: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        WhiteContainer(pad = PaddingValues(25.dp)) {
            if (title.isNotEmpty()) PageH1(title)
            content()
            if (back) BackLink("← بازگشت به منوی تنظیمات") { nav.popBackStack() }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ---------------------------------------------------------------
// منوی تنظیمات — ۱:۱ با /menu پنل وب
// ---------------------------------------------------------------
@Composable
fun SettingsMenuScreen(nav: NavHostController) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        WhiteContainer(pad = PaddingValues(25.dp)) {
            PageH1("⚙️ تنظیمات")
            MenuItemWeb("📡", "تنظیمات نقطه دسترسی") { nav.navigate("settings/ap") }
            MenuItemWeb("📱", "SIM800") { nav.navigate("settings/sim") }
            // [RFID-HIDE] تا وقتی هر دو تگ ثبت نشده‌اند منوی RFID دیده می‌شود؛
            // پس از پیکربندی کامل تگ‌ها (rfidConfigured) این آیتم از منو حذف
            // می‌شود و فقط با پاک‌کردن تگ‌ها (RFIDRESET) دوباره ظاهر می‌گردد.
            if (!AppModel.state.rfidConfigured) {
                MenuItemWeb("🔑", "RFID") { nav.navigate("settings/rfid") }
            }
            MenuItemWeb("🛰️", "تنظیمات GPS") { nav.navigate("settings/gps") }
            MenuItemWeb("🖥️", "سرور (VPS)") { nav.navigate("settings/vps") }
            BackLink("← بازگشت به صفحه کنترل") { nav.popBackStack() }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ---------------------------------------------------------------
// تنظیمات نقطه دسترسی — ۱:۱ با /settings پنل وب
// ---------------------------------------------------------------
@Composable
fun ApSettingsScreen(nav: NavHostController) {
    val s0 = AppModel.state
    var ssid by remember { mutableStateOf(s0.apSsid) }
    var pwd by remember { mutableStateOf(if (AppModel.mode == "ap") s0.apPwd else "") }
    var ip by remember { mutableStateOf(s0.apIp) }
    var thresh by remember { mutableStateOf(s0.sigThresh.toString()) }
    var msg by remember { mutableStateOf("") }
    var msgOk by remember { mutableStateOf<Boolean?>(null) }
    var threshMsg by remember { mutableStateOf("") }
    var threshOk by remember { mutableStateOf<Boolean?>(null) }
    var rebootAsk by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SettingsScaffold("📡 تنظیمات نقطه دسترسی", nav) {
        // ---------- فرم شبکه (همان فرم /save وب) ----------
        FormFieldWeb("نام شبکه وای‌فای (SSID)", ssid, { ssid = it })
        FormFieldWeb(
            "رمز عبور وای‌فای (حداقل ۸ نویسه)", pwd, { pwd = it },
            secret = true
        )
        FormFieldWeb("نشانی آی‌پی (مانند 192.168.4.1)", ip, { ip = it }, mono = true)
        BtnWeb("💾 ذخیره و راه‌اندازی مجدد") { rebootAsk = true }
        StatusMsg(msg, msgOk)

        Spacer(Modifier.height(16.dp))
        WarningBox(
            "⚠️ تغییر تنظیمات باعث راه‌اندازی مجدد دستگاه می‌شود. باید دوباره به شبکه وای‌فای جدید متصل شوید."
        )

        // ---------- جعبهٔ آستانهٔ سیگنال (همان threshold-box وب) ----------
        Spacer(Modifier.height(24.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .background(CC.cardBg, RoundedCornerShape(12.dp))
                .border(1.dp, CC.inputBorder, RoundedCornerShape(12.dp))
                .padding(18.dp)
        ) {
            Text(
                "📶 آستانه سیگنال",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = CC.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            )
            Text(
                "اگر محافظت خودکار سیگنال فعال باشد، خروجی‌ها در کمتر از این درصد خاموش می‌شوند و با بهبود سیگنال دوباره روشن می‌شوند.",
                fontSize = 13.sp,
                color = CC.label,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(12.dp))
            FormFieldWeb("آستانه سیگنال ضعیف (٪)", thresh, { thresh = it }, numeric = true)
            BtnWeb("💾 ذخیره آستانه", CC.green) {
                val v = thresh.toIntOrNull()
                if (v == null || v < 0 || v > 100) {
                    threshMsg = "مقدار نامعتبر است. عددی بین ۰ تا ۱۰۰ وارد کنید."
                    threshOk = false
                } else {
                    scope.launch {
                        val r = AppModel.saveSignal(v)
                        threshMsg = r
                        threshOk = !r.contains("✖")
                    }
                }
            }
            StatusMsg(threshMsg, threshOk)
        }
    }

    if (rebootAsk) {
        ConfirmWeb(
            title = "ذخیره و راه‌اندازی مجدد",
            text = "دستگاه ذخیره و ری‌استارت می‌شود؛ اگر SSID/IP را عوض کرده‌اید باید دوباره وصل شوید. ادامه؟",
            onConfirm = {
                rebootAsk = false
                scope.launch {
                    val r = AppModel.saveApCfg(ssid, pwd, ip)
                    msg = r
                    msgOk = !r.contains("✖")
                }
            },
            onDismiss = { rebootAsk = false }
        )
    }
}
