package com.carcontroller.app.ui.screens

// =====================================================================
// Home.kt — صفحهٔ اصلی:
//  نوار زبان + ردیف واحد آیکون‌های سیگنال به سبک نوار وضعیت گوشی
//  (وای‌فای/ابر، GSM، GPRS، GPS) + کپسول قطع خودکار
//  + کانتینر تیره با گرید ۲×۲ کارت خروجی‌ها + دو دکمهٔ آیکونی مرحلهٔ ۱/۲
//  تگ RFID (خودرو با درهای بسته/باز و پاور قرمز/سبز — بدون متن؛ مرحلهٔ ۲:
//  قرمز در حالت پیش‌فرض، سبز در حین عملیات/تأیید GPIO42) + دکمهٔ تنظیمات.
// =====================================================================

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.carcontroller.app.AppModel
import com.carcontroller.app.go
import com.carcontroller.app.ui.AutoDiscPill
import com.carcontroller.app.ui.CC
import com.carcontroller.app.ui.CarDoorsIcon
import com.carcontroller.app.ui.ConfirmWeb
import com.carcontroller.app.ui.LangPill
import com.carcontroller.app.ui.NameModal
import com.carcontroller.app.ui.OutputCard
import com.carcontroller.app.ui.PowerStatusIcon
import com.carcontroller.app.ui.SettingsBtn
import com.carcontroller.app.ui.StageIconBtn
import com.carcontroller.app.ui.StatusIconsRow
import com.carcontroller.app.ui.WhiteContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(nav: NavHostController) {
    val s = AppModel.state
    val scope = rememberCoroutineScope()
    var editPin by remember { mutableStateOf(-1) }
    var rfidAsk by remember { mutableStateOf(0) }
    var trunkFlash by remember { mutableStateOf(-1) }   // پالس لحظه‌ای صندوق (مثل جاوااسکریپت وب)

    DisposableEffect(Unit) {
        onDispose { }
    }

    // پالس لحظه‌ای صندوق: بعد از ۱ ثانیه خاموش می‌شود (مثل setTimeout جاوااسکریپت وب)
    LaunchedEffect(trunkFlash) {
        if (trunkFlash >= 0) {
            delay(1000)
            trunkFlash = -1
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ---------- top-bar: کپسول زبان (همان نوار بالای صفحهٔ وب) ----------
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // حالت سرور: دسترسی به رخدادها/پیامک‌ها/مسیر/صف (قابلیت داشبورد سرور)
            if (AppModel.mode == "vps") {
                androidx.compose.material3.TextButton(onClick = { nav.navigate("events") }) {
                    androidx.compose.material3.Text(
                        "📜 رخدادها",
                        color = CC.white,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
            // نشان روش اتصال — جایگزین نوار زبان در سمت دیگر
            LangPill(s.lang.ifEmpty { "fa" }) {
                val next = when (s.lang) { "fa" -> "en"; "en" -> "ar"; else -> "fa" }
                scope.launch { AppModel.showToast(AppModel.saveLang(next)) }
            }
        }

        // ---------- ردیف واحد آیکون‌های سیگنال (به سبک نوار وضعیت گوشی) ----------
        // وای‌فای/ابرِ سرور + آنتن GSM + نشان G (GPRS) + فلش GPS — همه در یک ردیف
        StatusIconsRow(
            mode = AppModel.mode,
            connected = AppModel.connected,
            rssiPct = s.rssiPct,
            gsmPct = s.gsmPct,
            gsmReg = s.gsmReg,
            gprsActive = s.gsmAtt || s.simIp.isNotEmpty(),
            gpsFix = s.gpsFix,
            sats = s.sats,
            gpsAlarm = s.safeAlarm
        )

        // ---------- کپسول قطع خودکار ----------
        AutoDiscPill(
            "⚠️ قطع و وصل خودکار رله‌ها با سیگنال ضعیف (کمتر از ${s.sigThresh}٪)",
            s.weakAuto
        ) { v ->
            scope.launch { AppModel.showToast(AppModel.saveWeakAuto(v)) }
        }

        // ---------- کانتینر اصلی: گرید ۲×۲ خروجی‌ها ----------
        Spacer(Modifier.height(6.dp))
        WhiteContainer {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    OutputCard(
                        index = 0,
                        name = s.names.getOrElse(0) { "خروجی 1" }.ifEmpty { "خروجی 1" },
                        on = s.outputs.getOrElse(0) { false } || trunkFlash == 0,
                        onToggle = { scope.launch { AppModel.setOutput(0, !s.outputs.getOrElse(0) { false }) } },
                        onEdit = { editPin = 0 }
                    )
                }
                Column(Modifier.weight(1f)) {
                    OutputCard(
                        index = 1,
                        name = s.names.getOrElse(1) { "خروجی 2" }.ifEmpty { "خروجی 2" },
                        on = s.outputs.getOrElse(1) { false } || trunkFlash == 1,
                        onToggle = {
                            scope.launch {
                                AppModel.setOutput(1, true)
                                trunkFlash = 1
                            }
                        },
                        onEdit = { editPin = 1 }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    OutputCard(
                        index = 2,
                        name = s.names.getOrElse(2) { "خروجی 3" }.ifEmpty { "خروجی 3" },
                        on = s.outputs.getOrElse(2) { false },
                        onToggle = { scope.launch { AppModel.setOutput(2, !s.outputs.getOrElse(2) { false }) } },
                        onEdit = { editPin = 2 }
                    )
                }
                Column(Modifier.weight(1f)) {
                    OutputCard(
                        index = 3,
                        name = s.names.getOrElse(3) { "خروجی 4" }.ifEmpty { "خروجی 4" },
                        on = s.outputs.getOrElse(3) { false },
                        onToggle = { scope.launch { AppModel.setOutput(3, !s.outputs.getOrElse(3) { false }) } },
                        onEdit = { editPin = 3 }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            // ---------- دو دکمهٔ مرحلهٔ ۱/۲ RFID — فقط آیکون (بدون متن) ----------
            // مرحلهٔ ۱: خروجی‌های قفل (پمپ/سوئیچ) LOW → خودرو با درهای بسته؛
            // پس از رفع قفل/عملیات موفق (HIGH) → خودرو با درهای باز.
            val stage1Open = s.outputs.getOrElse(2) { false } || s.outputs.getOrElse(3) { false }
            // مرحلهٔ ۲ (منطق معکوس): پیش‌فرض قرمز؛ با انتخاب و شروع عملیات
            // (پالس GPIO41 فعال) سبز می‌شود؛ تا زمانی که GPIO42 HIGH است سبز
            // می‌ماند و به‌محض LOW شدن پین ۴۲ دوباره قرمز می‌شود.
            // (st2 که قبلاً آیکون را «قفل‌شده روی سبز» می‌کرد دیگر اثر ندارد.)
            val stage2Ok = s.rfidP41 || s.rfidP42
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    StageIconBtn(onClick = { rfidAsk = 1 }) {
                        CarDoorsIcon(open = stage1Open, modifier = Modifier.size(width = 92.dp, height = 54.dp))
                    }
                }
                Column(Modifier.weight(1f)) {
                    StageIconBtn(onClick = { rfidAsk = 2 }) {
                        PowerStatusIcon(ok = stage2Ok, modifier = Modifier.size(46.dp))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SettingsBtn("⚙️ تنظیمات") { nav.navigate("settings") }
        }

        Spacer(Modifier.height(8.dp))
        // تغییر روش اتصال (AP ↔ سرور) — همیشه در دسترس
        androidx.compose.material3.TextButton(
            onClick = { nav.go("setup") },
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.material3.Text(
                "🔄 تغییر روش اتصال",
                color = CC.white,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    // ---------- مودال تغییر نام خروجی (همان nameModal وب) ----------
    if (editPin >= 0) {
        NameModal(
            title = "تغییر نام خروجی",
            initial = s.names.getOrElse(editPin) { "" },
            onConfirm = { nm ->
                val pin = editPin
                editPin = -1
                if (nm.isNotEmpty()) scope.launch { AppModel.showToast(AppModel.renameOutput(pin, nm)) }
            },
            onDismiss = { editPin = -1 }
        )
    }

    // ---------- تأیید اجرای مرحلهٔ RFID (مودال — متن فقط داخل همین پنجره) ----------
    if (rfidAsk > 0) {
        ConfirmWeb(
            title = if (rfidAsk == 1) "اجرای مرحلهٔ ۱ تگ RFID" else "اجرای مرحلهٔ ۲ تگ RFID",
            text = if (rfidAsk == 1)
                "مثل نگه‌داشتن کارت ۲ ثانیه: رفع قفل + باز شدن درب‌ها. ادامه؟"
            else
                "پالس استارت (GPIO41) حداکثر ۵ ثانیه فعال می‌ماند؛ آیکون پاور با شروع عملیات سبز می‌شود و تا فعال‌بودن GPIO42 سبز می‌ماند. ادامه؟",
            onConfirm = {
                val st = rfidAsk
                rfidAsk = 0
                scope.launch { AppModel.showToast(AppModel.rfidStage(st)) }
            },
            onDismiss = { rfidAsk = 0 }
        )
    }
}
