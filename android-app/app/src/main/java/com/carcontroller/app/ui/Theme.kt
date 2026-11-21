package com.carcontroller.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// =====================================================================
// Theme.kt — پوستهٔ تاریک (مشکی) — پس‌زمینهٔ همهٔ منوها و منوی اصلی
// ---------------------------------------------------------------------
// [UI-v6] به‌جای گرادیان بنفش، پس‌زمینهٔ اپ کاملاً مشکی است؛
// کانتینر صفحات #161616 با گوشهٔ ۲۰dp و قاب ظریف، کارت‌های داخلی #232323،
// فیلدهای ورودی #262626، متن‌های روشن (#E8E8E8/#9E9E9E) و رنگ‌های تأکید
// (بنفش/سبز/نارنجی/قرمز) برای خوانایی کامل روی زمینهٔ تاریک.
// =====================================================================

object CC {
    /** پس‌زمینهٔ اپ — مشکی (به‌جای گرادیان بنفش مرحلهٔ ۴) */
    val bg = Color(0xFF000000)
    val grad1 = Color(0xFF000000)
    val grad2 = Color(0xFF070707)
    val primary = Color(0xFF7986EB)
    val primaryDark = Color(0xFF5A67D8)
    val green = Color(0xFF4CAF50)
    val greenDark = Color(0xFF43A047)
    val orange = Color(0xFFFF9800)
    val orangeDark = Color(0xFFFB8C00)
    val red = Color(0xFFF44336)
    val redDark = Color(0xFFE53935)
    /** زمینهٔ کانتینر اصلی صفحات (قبلاً سفید بود) */
    val surface = Color(0xFF161616)
    /** قاب ظریف کانتینر روی زمینهٔ مشکی */
    val surfaceBorder = Color(0xFF2A2A2A)
    /** کارت‌های داخلی (status-card، menu-item، کارت خروجی‌ها و…) */
    val cardBg = Color(0xFF232323)
    val cardHover = Color(0xFF2E2E2E)
    /** سفید — فقط برای متن روی دکمه‌های رنگی و اجزای روشن */
    val white = Color(0xFFFFFFFF)
    val text = Color(0xFFE8E8E8)
    val label = Color(0xFF9E9E9E)
    val labelDark = Color(0xFFB4B4B4)
    val hintC = Color(0xFF989898)
    val ok = Color(0xFF66BB6A)
    val warn = Color(0xFFFFA726)
    val err = Color(0xFFEF5350)
    val line = Color(0xFF333333)
    val inputBorder = Color(0xFF3D3D3D)
    /** زمینهٔ فیلدهای ورودی */
    val inputBg = Color(0xFF262626)
    val infoBg = Color(0xFF1B2138)
    val infoBorder = Color(0xFF39415E)
    val legend = Color(0xFF9AA8F0)
    val gold = Color(0xFFFFD700)
    val white20 = Color(0x33FFFFFF)
    val white25 = Color(0x40FFFFFF)
    val black25 = Color(0x40000000)
    val black20 = Color(0x33000000)
    /** زمینهٔ پیام کوتاه سراسری (تویست) روی پس‌زمینهٔ مشکی */
    val toastBg = Color(0xF2202020)
    val yellow = Color(0xFFFEB03B)
}

private val DarkScheme = darkColorScheme(
    primary = CC.primary,
    onPrimary = CC.white,
    secondary = CC.primaryDark,
    tertiary = CC.green,
    error = CC.err,
    background = CC.bg,
    surface = CC.surface,
    onSurface = CC.text,
    onSurfaceVariant = CC.label,
    outline = CC.inputBorder,
    surfaceVariant = CC.cardBg
)

@Composable
fun CCTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}

/** پس‌زمینهٔ اپ — مشکی تمام‌صفحه (پوستهٔ تاریک UI-v6) */
fun ccGradient(): Brush = Brush.linearGradient(listOf(CC.grad1, CC.grad2))

/** barColor وب: قرمز <۳۰٪، زرد <=۶۵٪، سبز >۶۵٪ */
fun barColorOf(pct: Int): Color =
    if (pct < 30) CC.red else if (pct <= 65) CC.yellow else CC.green

/**
 * قاب صفحه — همان ساختار HTML پنل وب:
 *  body مشکی + ستون وسط‌چین با حداکثر عرض ۶۰۰dp (مثل max-width:600px)
 *  و مقیاس خودکار نسبت به اندازهٔ صفحهٔ گوشی (responsive).
 */
@Composable
fun ScreenFrame(
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(ccGradient()),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(Modifier.widthIn(max = 600.dp).fillMaxSize(), content = content)
    }
}
