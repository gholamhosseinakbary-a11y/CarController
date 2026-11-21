package com.carcontroller.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// =====================================================================
// Widgets.kt — اجزای رابط پوستهٔ تاریک (UI-v6)
// ---------------------------------------------------------------------
// همان اجزا با نقشهٔ رنگی جدید روی زمینهٔ مشکی:
//   .container → WhiteContainer (سطح تیرهٔ #161616 + قاب ظریف)
//   .card → OutputCard (#232323) · .menu-item → MenuItemWeb
//   .status-row → StatusRowWeb · .form-group/label/input → FormFieldWeb
//   .switch-row → SwitchRowWeb · .hint → HintWeb
//   .warning → WarningBox (کهربایی تیره) · .info → InfoBox (سرمه‌ای تیره)
//   button/.btn-green/.btn-orange → BtnWeb (رنگ‌های تأکید بدون تغییر)
// =====================================================================

/** .container — کانتینر اصلی تیرهٔ صفحه (radius 20 + قاب ظریف روی زمینهٔ مشکی) */
@Composable
fun WhiteContainer(
    pad: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0x16000000),
                spotColor = Color(0x16000000)
            )
            .background(CC.surface, RoundedCornerShape(20.dp))
            .border(1.dp, CC.surfaceBorder, RoundedCornerShape(20.dp))
            .padding(pad),
        content = content
    )
}

/** h1 صفحه — وسط‌چین، روشن، درشت */
@Composable
fun PageH1(text: String) {
    Text(
        text,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = CC.text,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
    )
}

/** .section-title — درشت با خط زیرین بنفش (border-bottom:2px) */
@Composable
fun SectionTitleWeb(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 10.dp)
    ) {
        Text(
            text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = CC.text
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 5.dp)
                .height(2.dp)
                .background(CC.primary)
        )
    }
}

/** .status-card — کارت خاکستری ردیف‌های وضعیت */
@Composable
fun StatusCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(CC.cardBg, RoundedCornerShape(15.dp))
            .padding(15.dp),
        content = content
    )
}

/** .status-row — ردیف «برچسب | مقدار» با جداکنندهٔ نازک */
@Composable
fun StatusRowWeb(label: String, value: String, tone: Color = CC.text, last: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CC.label)
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = tone,
            textAlign = TextAlign.Left
        )
    }
    if (!last) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CC.line)
        )
    }
}

/** label + input — دقیقاً فرم وب: خط ۲dp خاکستری و با فوکوس بنفش */
@Composable
fun FormFieldWeb(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    numeric: Boolean = false,
    secret: Boolean = false,
    multiLine: Boolean = false,
    mono: Boolean = false
) {
    val focus = remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(bottom = 15.dp)) {
        if (label.isNotEmpty()) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CC.labelDark,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        val border = if (focus.value) CC.primary else CC.inputBorder
        Box(
            Modifier
                .fillMaxWidth()
                .border(2.dp, border, RoundedCornerShape(10.dp))
                .background(CC.inputBg)
                .padding(horizontal = 11.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = !multiLine,
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = CC.text,
                    fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
                ),
                keyboardOptions = if (numeric) androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ) else androidx.compose.foundation.text.KeyboardOptions.Default,
                visualTransformation = if (secret)
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
                else androidx.compose.ui.text.input.VisualTransformation.None,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(CC.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focus.value = it.isFocused }
            ) { inner ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, fontSize = 15.sp, color = CC.hintC)
                }
                inner()
            }
        }
    }
}

/** .switch-row — ردیف خاکستری + چک‌باکس (رنگ تأیید قابل انتخاب) */
@Composable
fun SwitchRowWeb(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    accent: Color = CC.green
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(CC.cardBg, RoundedCornerShape(12.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = CC.text,
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp)
        )
        Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            enabled = true,
            colors = CheckboxDefaults.colors(
                checkedColor = accent,
                uncheckedColor = Color(0xFF999999),
                checkmarkColor = Color.White
            ),
            modifier = Modifier.size(28.dp)
        )
    }
}

/** .hint — متن کمکی خاکستری */
@Composable
fun HintWeb(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = CC.hintC,
        lineHeight = 22.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

/** .warning — جعبهٔ هشدار کهربایی روی زمینهٔ تیره */
@Composable
fun WarningBox(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF332908), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(text, fontSize = 13.sp, color = Color(0xFFFFCB4F), lineHeight = 22.sp)
    }
}

/** .info — جعبهٔ آبی-بنفش راهنما (صفحهٔ RFID) */
@Composable
fun InfoBox(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(CC.infoBg, RoundedCornerShape(12.dp))
            .padding(13.dp)
    ) {
        Text(text, fontSize = 14.sp, color = CC.text, lineHeight = 26.sp)
    }
}

/** .success — جعبهٔ سبز تأیید (صفحهٔ RFID) روی زمینهٔ تیره */
@Composable
fun SuccessBox(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F2E1B), RoundedCornerShape(11.dp))
            .padding(14.dp)
    ) {
        Text(text, fontSize = 14.sp, color = Color(0xFF7ED99A))
    }
}

/** button — دکمهٔ تمام‌عرض وب با همان رنگ‌ها */
@Composable
fun BtnWeb(
    text: String,
    color: Color = CC.primary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val bg = if (enabled) color else Color(0x55999999)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .height(52.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CC.white)
    }
}

/** .settings-btn — دکمهٔ بزرگ ⚙️ تنظیمات صفحهٔ اصلی */
@Composable
fun SettingsBtn(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(CC.primary, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CC.white)
    }
}

/** .menu-item — ردیف منوی تنظیمات (آیکون + متن + فلش) */
@Composable
fun MenuItemWeb(emoji: String, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(15.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000)
            )
            .background(CC.cardBg, RoundedCornerShape(15.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 26.sp)
        Spacer(Modifier.width(14.dp))
        Text(
            text,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = CC.text,
            modifier = Modifier.weight(1f)
        )
        Text("◀", fontSize = 17.sp, color = CC.primary, fontWeight = FontWeight.Bold)
    }
}

/** .back-link — لینک بازگشت وسط‌چین بنفش */
@Composable
fun BackLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = CC.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(top = 18.dp)
    )
}

/** .status.on/.off — نشان روشن/خاموش روی کارت خروجی */
@Composable
fun StatusPill(on: Boolean, onText: String, offText: String) {
    Text(
        if (on) "$onText ✓" else "$offText ○",
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        color = CC.white,
        modifier = Modifier
            .background(if (on) CC.green else CC.red, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    )
}

/**
 * متن وضعیت هر خروجی بر حسب «شمارهٔ سخت‌افزاری» (نه نام سفارشی) —
 * به‌جای «روشن/خاموش» برای هر خروجی جفت متن معنادار خودش:
 *   0 درب‌ها:   HIGH=قفل / LOW=باز     · ۱ صندوق عقب: HIGH(پالس)=باز / LOW=بسته
 *   2 پمپ بنزین: HIGH=وصل / LOW=قطع    · ۳ سوئیچ خودرو: HIGH=باز / LOW=بسته
 */
fun outputStateTexts(index: Int): Pair<String, String> = when (index) {
    0 -> "قفل" to "باز"
    1 -> "باز" to "بسته"
    2 -> "وصل" to "قطع"
    3 -> "باز" to "بسته"
    else -> "روشن" to "خاموش"
}

/** .card — کارت خروجی صفحهٔ اصلی (نام + ✏️ + وضعیت با متن مخصوص همان خروجی) */
@Composable
fun OutputCard(
    index: Int,
    name: String,
    on: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val (onText, offText) = outputStateTexts(index)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(15.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000)
            )
            .background(CC.cardBg, RoundedCornerShape(15.dp))
            .clickable { onToggle() }
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CC.text,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(
                "✏️",
                fontSize = 14.sp,
                color = CC.labelDark,
                modifier = Modifier
                    .clickable { onEdit() }
                    .padding(4.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        StatusPill(on, onText, offText)
    }
}

// =====================================================================
// [UI-v6] آیکون‌های مرحلهٔ ۱/۲ تگ RFID — دکمه‌های صفحهٔ اصلی «بدون متن»
// ---------------------------------------------------------------------
//   مرحلهٔ ۱: آیکون خودرو — درهای بسته (پیش‌فرض/خروجی‌های قفل LOW)
//              → درهای باز پس از عملیات موفق (پمپ/سوئیچ HIGH = قفل باز)
//   مرحلهٔ ۲: آیکون پاور — قرمز به‌صورت پیش‌فرض؛ با انتخاب و شروع عملیات
//              (پالس GPIO41) سبز می‌شود و تا وقتی GPIO42 HIGH است سبز می‌ماند؛
//              به‌محض LOW شدن پین ۴۲ دوباره قرمز می‌شود.
// =====================================================================

/** خودروی نما از کنار — open=true هر دو در باز (پنل‌های سبز)، وگرنه خط درزِ بسته */
@Composable
fun CarDoorsIcon(open: Boolean, modifier: Modifier = Modifier.size(width = 88.dp, height = 52.dp)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val body = Color(0xFF5E6D7D)
        val glass = Color(0xFFBFD9EA)
        val wheel = Color(0xFF3A434E)
        val hub = Color(0xFFE9EEF2)
        val doorOpen = CC.green

        // بدنه
        drawRoundRect(
            body, cornerRadius = CornerRadius(h * 0.10f),
            topLeft = Offset(w * 0.04f, h * 0.48f),
            size = Size(w * 0.92f, h * 0.36f)
        )
        // کابین
        drawRoundRect(
            body, cornerRadius = CornerRadius(h * 0.08f),
            topLeft = Offset(w * 0.20f, h * 0.14f),
            size = Size(w * 0.52f, h * 0.44f)
        )
        // شیشه‌ها
        drawRoundRect(
            glass, cornerRadius = CornerRadius(h * 0.05f),
            topLeft = Offset(w * 0.24f, h * 0.20f),
            size = Size(w * 0.20f, h * 0.28f)
        )
        drawRoundRect(
            glass, cornerRadius = CornerRadius(h * 0.05f),
            topLeft = Offset(w * 0.47f, h * 0.20f),
            size = Size(w * 0.21f, h * 0.28f)
        )
        // چرخ‌ها
        drawCircle(wheel, radius = h * 0.115f, center = Offset(w * 0.24f, h * 0.84f))
        drawCircle(hub, radius = h * 0.055f, center = Offset(w * 0.24f, h * 0.84f))
        drawCircle(wheel, radius = h * 0.115f, center = Offset(w * 0.78f, h * 0.84f))
        drawCircle(hub, radius = h * 0.055f, center = Offset(w * 0.78f, h * 0.84f))

        if (!open) {
            // درزِ درِ بسته (خط عمودی وسط کابین) + دستگیره
            drawLine(
                color = Color(0xFF3A434E),
                start = Offset(w * 0.465f, h * 0.16f),
                end = Offset(w * 0.465f, h * 0.80f),
                strokeWidth = w * 0.012f
            )
            drawRoundRect(
                Color(0xFF3A434E), cornerRadius = CornerRadius(w * 0.004f),
                topLeft = Offset(w * 0.48f, h * 0.42f),
                size = Size(w * 0.05f, h * 0.05f)
            )
        } else {
            // هر دو در باز: پنل‌های بازشده به بیرون (سبز) از لولای وسط
            val dw = w * 0.22f
            val dh = h * 0.40f
            // درِ جلو (سمت راست) — حول لولای پایین-جلو چرخیده به بیرون
            rotate(degrees = 34f, pivot = Offset(w * 0.50f, h * 0.78f)) {
                drawRoundRect(
                    doorOpen, cornerRadius = CornerRadius(w * 0.010f),
                    topLeft = Offset(w * 0.50f, h * 0.78f - dh),
                    size = Size(dw, dh)
                )
            }
            // درِ عقب (سمت چپ) — حول لولای پایین-عقب چرخیده به بیرون
            rotate(degrees = -34f, pivot = Offset(w * 0.50f, h * 0.78f)) {
                drawRoundRect(
                    doorOpen, cornerRadius = CornerRadius(w * 0.010f),
                    topLeft = Offset(w * 0.50f - dw, h * 0.78f - dh),
                    size = Size(dw, dh)
                )
            }
        }
    }
}

/**
 * آیکون پاور (⏻) — [UI-v6] منطق معکوس رنگ:
 *   قرمز = پیش‌فرض / پایان عملیات (GPIO42 LOW)
 *   سبز  = عملیات استارت شروع شده (پالس GPIO41 فعال) یا GPIO42 HIGH
 */
@Composable
fun PowerStatusIcon(ok: Boolean, modifier: Modifier = Modifier.size(42.dp)) {
    val color = if (ok) CC.green else CC.red
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.width * 0.36f
        val stroke = size.width * 0.11f
        // حلقهٔ باز از بالا (شکل کلاسیک پاور)
        drawArc(
            color = color,
            startAngle = 300f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(cx - r, cy - r),
            size = Size(r * 2f, r * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        // میلهٔ عمودی از شکاف بالا تا زیر مرکز
        drawLine(
            color = color,
            start = Offset(cx, cy - r),
            end = Offset(cx, cy + r * 0.25f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

/** دکمهٔ مرحله (فقط آیکون — بدون متن) روی کارت روشن */
@Composable
fun StageIconBtn(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(84.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(15.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000)
            )
            .background(CC.cardBg, RoundedCornerShape(15.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

// =====================================================================
// [UI-v5] نشانگرهای سیگنال به سبک «نوار وضعیت گوشی هوشمند» — یک ردیف
// ---------------------------------------------------------------------
// به‌جای کپسول‌های جداگانهٔ نوار سیگنال (وای‌فای/GSM/GPRS/GPS)، همهٔ
// نشانگرها به‌صورت آیکون‌های نوار وضعیت گوشی در «یک ردیف» نمایش داده می‌شوند:
//   وای‌فای سه‌کمانه (شدت = تعداد کمان پر) · آنتن سلولی GSM چهارمیله
//   نشان G برای GPRS · فلش موقعیت GPS · (حالت سرور: آیکون ابر به‌جای وای‌فای)
// رنگ هر آیکون همان منطق قبلی کیفیت است: قرمز <۳۰٪ / زرد ≤۶۵٪ / سبز >۶۵٪.
// =====================================================================

/** آیکون وای‌فای گوشی — نقطه + سه کمان؛ level=تعداد کمان‌های پر (۰..۳) */
@Composable
fun WifiSignalIcon(level: Int, color: Color, modifier: Modifier = Modifier.size(26.dp)) {
    val dim = Color(0x59FFFFFF)
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.80f
        drawCircle(color = if (level > 0) color else dim, radius = size.width * 0.07f, center = Offset(cx, cy))
        val stroke = size.width * 0.10f
        val arcs = listOf(
            Pair(size.width * 0.24f, 47f),
            Pair(size.width * 0.37f, 76f),
            Pair(size.width * 0.50f, 104f)
        )
        arcs.forEachIndexed { i, (r, sweep) ->
            drawArc(
                color = if (i < level) color else dim,
                startAngle = 270f - sweep / 2f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

/** آیکون ابر — وضعیت سرور در حالت VPS (سبز = متصل، قرمز = قطع) */
@Composable
fun CloudStatusIcon(ok: Boolean, modifier: Modifier = Modifier.size(26.dp)) {
    val color = if (ok) CC.green else CC.red
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val r1 = w * 0.26f
        drawCircle(color, radius = r1, center = Offset(w * 0.36f, h * 0.46f))
        drawCircle(color, radius = r1 * 0.86f, center = Offset(w * 0.62f, h * 0.40f))
        drawCircle(color, radius = r1 * 0.66f, center = Offset(w * 0.80f, h * 0.52f))
        drawRoundRect(
            color, cornerRadius = CornerRadius(w * 0.05f),
            topLeft = Offset(w * 0.24f, h * 0.52f),
            size = Size(w * 0.52f, h * 0.18f)
        )
    }
}

/** آیکون آنتن سلولی گوشی — چهار میله صعودی؛ bars=تعداد میله‌های پر (۰..۴) */
@Composable
fun CellSignalIcon(bars: Int, color: Color, modifier: Modifier = Modifier.size(26.dp)) {
    val dim = Color(0x59FFFFFF)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val gap = w * 0.08f
        val bw = (w - gap * 3f) / 4f
        for (i in 0 until 4) {
            val bh = h * (i + 1) / 4f
            drawRoundRect(
                color = if (i < bars) color else dim,
                topLeft = Offset(i * (bw + gap), h - bh),
                size = Size(bw, bh),
                cornerRadius = CornerRadius(bw * 0.30f, bw * 0.30f)
            )
        }
    }
}

/** نشان G — آیکون GPRS روی گوشی‌های همراه (سبز = متصل GPRS، کم‌رنگ = قطع) */
@Composable
fun GprsBadgeIcon(active: Boolean) {
    Text(
        "G",
        fontSize = 17.sp,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.SansSerif,
        color = if (active) CC.green else Color(0x59FFFFFF)
    )
}

/** آیکون فلش موقعیت GPS — مثل نشان موقعیت نوار وضعیت گوشی */
@Composable
fun GpsNavIcon(color: Color, modifier: Modifier = Modifier.size(26.dp)) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val p = Path().apply {
            moveTo(w * 0.50f, h * 0.04f)
            lineTo(w * 0.90f, h * 0.82f)
            lineTo(w * 0.50f, h * 0.62f)
            lineTo(w * 0.10f, h * 0.82f)
            close()
        }
        drawPath(p, color)
    }
}

/** سطح ۰..۳ از درصد (وای‌فای) و ۰..۴ میله از درصد (GSM) */
fun levelOf3(pct: Int): Int = when { pct > 65 -> 3; pct > 30 -> 2; pct > 0 -> 1; else -> 0 }
fun barsOf4(pct: Int): Int = when { pct >= 75 -> 4; pct >= 50 -> 3; pct >= 25 -> 2; pct > 0 -> 1; else -> 0 }

/** ردیف واحد آیکون‌های وضعیت — جایگزین کپسول‌های نوار سیگنال و سطر GPRS */
@Composable
fun StatusIconsRow(
    mode: String,            // "ap" | "vps"
    connected: Boolean,      // vps: متصل به سرور؟ / ap: دستگاه پاسخ می‌دهد
    rssiPct: Int,           // شدت وای‌فای اکسس‌پوینت (٪)
    gsmPct: Int,            // سیگنال GSM (٪)
    gsmReg: Boolean,        // ثبت شبکه
    gprsActive: Boolean,    // GPRS متصل (att/IP)
    gpsFix: Boolean,
    sats: Int,
    gpsAlarm: Boolean       // آلارم محدودهٔ امن → آیکون GPS قرمز
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(CC.white20, RoundedCornerShape(30.dp))
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ۱) وای‌فای (حالت AP) یا ابر سرور (حالت VPS)
        if (mode == "ap") {
            WifiSignalIcon(levelOf3(rssiPct), barColorOf(rssiPct))
        } else {
            CloudStatusIcon(connected)
        }
        // ۲) آنتن GSM (بی‌ثبت‌بودن شبکه = همهٔ میله‌ها کم‌رنگ)
        CellSignalIcon(if (gsmReg || gsmPct > 0) barsOf4(gsmPct) else 0, barColorOf(gsmPct))
        // ۳) GPRS (نشان G)
        GprsBadgeIcon(active = gprsActive)
        // ۴) GPS
        GpsNavIcon(
            color = if (gpsAlarm) CC.red
            else if (!gpsFix) CC.red
            else if (sats >= 8) CC.green
            else if (sats >= 5) CC.yellow
            else CC.orange
        )
    }
}

/** .auto-disconnect-container — کپسول چک‌باکس قطع خودکار */
@Composable
fun AutoDiscPill(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .background(CC.white25, RoundedCornerShape(30.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(
                checkedColor = CC.red,
                uncheckedColor = CC.white,
                checkmarkColor = CC.white
            ),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CC.white)
    }
}

/** .lang-select — کپسول زبان طلایی (نوار بالای صفحه) روی زمینهٔ مشکی */
@Composable
fun LangPill(lang: String, onClick: () -> Unit) {
    val name = when (lang) { "en" -> "English"; "ar" -> "العربية"; else -> "فارسی" }
    Box(
        Modifier
            .background(CC.white20, RoundedCornerShape(30.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        Text(name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CC.gold)
    }
}

/** .mac-display — جعبهٔ تیره با قاب بنفش (صفحهٔ فعال‌سازی) */
@Composable
fun MacBox(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .background(CC.inputBg, RoundedCornerShape(10.dp))
            .border(2.dp, CC.primary, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(
            text,
            fontSize = 17.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = CC.text,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

/** fieldset/legend — قاب تگ‌های RFID */
@Composable
fun FieldsetWeb(legend: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp)
            .border(1.dp, CC.infoBorder, RoundedCornerShape(14.dp))
            .padding(17.dp)
    ) {
        Text(legend, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CC.legend)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

/** #statusMsg — پیام نتیجهٔ ذخیره (وسط‌چین و درشت) */
@Composable
fun StatusMsg(text: String, ok: Boolean? = null) {
    if (text.isEmpty()) return
    Text(
        text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = when (ok) { true -> CC.ok; false -> CC.err; null -> CC.label },
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    )
}

/** .alarm-banner — بنر قرمز هشدار محدودهٔ امن */
@Composable
fun AlarmBanner(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .background(CC.red, RoundedCornerShape(12.dp))
            .padding(15.dp)
    ) {
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = CC.white,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** .modal-box — دیالوگ نام خروجی (همان مودال وب) */
@Composable
fun NameModal(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CC.surface,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CC.text)
        },
        text = {
            FormFieldWeb(label = "", value = value, onChange = { value = it })
        },
        confirmButton = {
            Box(
                Modifier
                    .background(CC.green, RoundedCornerShape(10.dp))
                    .clickable { onConfirm(value.trim()) }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) { Text("ذخیره", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CC.white) }
        },
        dismissButton = {
            Box(
                Modifier
                    .background(CC.red, RoundedCornerShape(10.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) { Text("انصراف", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CC.white) }
        }
    )
}

/** Confirm دیالوگ عمومی (تأیید/انصراف) */
@Composable
fun ConfirmWeb(
    title: String,
    text: String,
    confirmText: String = "تأیید و اجرا",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CC.surface,
        shape = RoundedCornerShape(18.dp),
        title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CC.text) },
        text = { Text(text, fontSize = 14.sp, color = CC.labelDark, lineHeight = 22.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, fontWeight = FontWeight.Bold, color = CC.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", fontWeight = FontWeight.Bold, color = CC.label)
            }
        }
    )
}
