package com.carcontroller.app.ui.screens

// =====================================================================
// Events.kt — رخدادها / پیامک‌ها / مسیر GPS / صف فرمان‌ها (حالت سرور)
// ---------------------------------------------------------------------
// همان زبان طراحی پنل: کانتینر سفید + کارت‌های #f8f9fa + تب‌های بنفش.
// =====================================================================

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.carcontroller.app.AppModel
import com.carcontroller.app.arr
import com.carcontroller.app.dbl
import com.carcontroller.app.int
import com.carcontroller.app.lng
import com.carcontroller.app.objs
import com.carcontroller.app.str
import com.carcontroller.app.ui.BackLink
import com.carcontroller.app.ui.BtnWeb
import com.carcontroller.app.ui.CC
import com.carcontroller.app.ui.PageH1
import com.carcontroller.app.ui.SectionTitleWeb
import com.carcontroller.app.ui.StatusMsg
import com.carcontroller.app.ui.WhiteContainer
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** کارت خاکستری یک آیتم فهرست — همان .sms-item/#f8f9fa وب */
@Composable
private fun ItemCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(CC.cardBg, RoundedCornerShape(10.dp))
            .padding(10.dp),
        content = content
    )
}

private fun tsFmt(ts: Long): String {
    if (ts <= 0) return "—"
    return try {
        SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).format(Date(ts * 1000))
    } catch (_: Exception) { ts.toString() }
}

@Composable
private fun TabButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) Color.White else CC.label,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .background(
                if (selected) CC.primary else CC.cardBg,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

// ---------------------------------------------------------------
// رخدادها / پیامک‌ها / مسیر
// ---------------------------------------------------------------
@Composable
fun EventsScreen(nav: NavHostController) {
    var tab by remember { mutableStateOf(0) }        // 0 events | 1 sms | 2 locations
    var events by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var sms by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var locs by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val dev = AppModel.deviceId

    fun reload() {
        scope.launch {
            try {
                when (tab) {
                    0 -> events = AppModel.vps().events(dev)
                    1 -> sms = AppModel.vps().sms(dev)
                    else -> locs = AppModel.vps().locations(dev)
                }
                msg = ""
            } catch (e: Exception) {
                msg = (e as? com.carcontroller.app.ApiException)?.message ?: "ارتباط برقرار نشد"
            }
        }
    }

    LaunchedEffect(tab) { reload() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        WhiteContainer(pad = PaddingValues(20.dp)) {
            PageH1("📜 رخدادها و پیامک‌ها")

            Row(Modifier.padding(bottom = 8.dp)) {
                TabButton("رخدادها", tab == 0) { tab = 0 }
                TabButton("پیامک‌ها", tab == 1) { tab = 1 }
                TabButton("مسیر GPS", tab == 2) { tab = 2 }
            }

            when (tab) {
                0 -> {
                    if (events.isEmpty()) HintGray("رخدادی ثبت نشده — «به‌روزرسانی» را بزنید")
                    events.forEach { e ->
                        ItemCard {
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    e.str("kind").ifEmpty { e.str("k") },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CC.text,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(tsFmt(e.lng("ts")), fontSize = 11.sp, color = CC.hintC)
                            }
                            val d = e.opt("data")
                            val dTxt = when (d) {
                                is JSONObject -> d.toString()
                                else -> d?.toString() ?: ""
                            }
                            if (dTxt.isNotEmpty()) Text(dTxt, fontSize = 12.sp, color = CC.label)
                        }
                    }
                }

                1 -> {
                    if (sms.isEmpty()) HintGray("پیامکی ثبت نشده — «به‌روزرسانی» را بزنید")
                    sms.forEach { m ->
                        ItemCard {
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    "📱 ${m.str("from")}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CC.text,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(tsFmt(m.lng("ts")), fontSize = 11.sp, color = CC.hintC)
                            }
                            Text(m.str("text"), fontSize = 13.sp, color = CC.text)
                            val cls = m.int("cls")
                            Text(
                                when (cls) {
                                    1 -> "فرمان اجرا شد"
                                    2 -> "فرمان نامعتبر"
                                    3 -> "ناشناخته"
                                    else -> "دریافتی"
                                },
                                fontSize = 11.sp,
                                color = when (cls) {
                                    1 -> CC.ok
                                    2 -> CC.err
                                    else -> CC.label
                                }
                            )
                        }
                    }
                }

                else -> {
                    if (locs.isEmpty()) {
                        HintGray("موقعیتی ثبت نشده — «به‌روزرسانی» را بزنید.")
                    } else {
                        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                            Text(
                                "تعداد نقاط: ${locs.size}",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CC.label,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        val last = locs.firstOrNull()
                        if (last != null) {
                            Text(
                                "آخرین موقعیت: " + String.format(Locale.US, "%.6f, %.6f", last.dbl("lat"), last.dbl("lng")),
                                fontSize = 13.sp, color = CC.label
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "🗺️ باز کردن در نقشه",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CC.primary,
                                modifier = Modifier
                                    .clickable {
                                        val la = last.dbl("lat"); val lo = last.dbl("lng")
                                        try {
                                            ctx.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$la,$lo"))
                                            )
                                        } catch (_: Exception) { }
                                    }
                                    .padding(bottom = 8.dp)
                            )
                        }
                        LocationPlot(locs.map { Pair(it.dbl("lat"), it.dbl("lng")) })
                    }
                }
            }

            BtnWeb("🔄 به‌روزرسانی") { reload() }
            StatusMsg(msg, false)
            BackLink("← بازگشت") { nav.popBackStack() }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun HintGray(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = CC.hintC,
        lineHeight = 20.sp,
        modifier = Modifier.padding(vertical = 6.dp)
    )
}

/** رسم سادهٔ مسیر روی Canvas (بدون نیاز به کتابخانهٔ نقشه) — با رنگ‌های پنل */
@Composable
private fun LocationPlot(points: List<Pair<Double, Double>>) {
    if (points.isEmpty()) return
    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(CC.surface, RoundedCornerShape(15.dp))
    ) {
        Canvas(Modifier.fillMaxWidth().height(220.dp).padding(10.dp)) {
            if (points.size < 2) {
                drawCircle(CC.primaryDark, radius = 6f, center = Offset(size.width / 2f, size.height / 2f))
                return@Canvas
            }
            val lats = points.map { it.first }
            val lngs = points.map { it.second }
            var minLa = lats.minOrNull() ?: 0.0; var maxLa = lats.maxOrNull() ?: 0.0
            var minLo = lngs.minOrNull() ?: 0.0; var maxLo = lngs.maxOrNull() ?: 0.0
            if (maxLa - minLa < 1e-9) { minLa -= 0.0005; maxLa += 0.0005 }
            if (maxLo - minLo < 1e-9) { minLo -= 0.0005; maxLo += 0.0005 }
            val w = size.width
            val h = size.height
            fun toXY(p: Pair<Double, Double>): Offset {
                val x = ((p.second - minLo) / (maxLo - minLo)).toFloat() * w
                val y = (1f - ((p.first - minLa) / (maxLa - minLa)).toFloat()) * h
                return Offset(x, y)
            }
            val path = Path()
            points.forEachIndexed { i, p ->
                val o = toXY(p)
                if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
            }
            drawPath(path, CC.primary, style = Stroke(width = 4f))
            points.forEach { p -> drawCircle(CC.primaryDark, radius = 6f, center = toXY(p)) }
            // آخرین نقطه (اول در لیست) سبز
            toXY(points.first()).let { drawCircle(CC.ok, radius = 9f, center = it) }
        }
    }
}

// ---------------------------------------------------------------
// صف فرمان‌ها
// ---------------------------------------------------------------
@Composable
fun QueueScreen(nav: NavHostController) {
    var pending by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var recent by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            try {
                val q = AppModel.vps().queue(AppModel.deviceId)
                pending = q.arr("queue")?.objs() ?: emptyList()
                recent = q.arr("recent")?.objs() ?: emptyList()
                msg = ""
            } catch (e: Exception) {
                msg = (e as? com.carcontroller.app.ApiException)?.message ?: "ارتباط برقرار نشد"
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        WhiteContainer(pad = PaddingValues(20.dp)) {
            PageH1("📥 صف فرمان‌ها")
            Text(
                "فرمان‌های در انتظار دستگاه (آفلاین) و اخیر",
                fontSize = 13.sp,
                color = CC.hintC,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            SectionTitleWeb("در انتظار (${pending.size})")
            pending.forEach { c ->
                ItemCard {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            "⏳ ${c.str("c")}",
                            fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CC.text,
                            modifier = Modifier.weight(1f)
                        )
                        Text(tsFmt(c.lng("created_at")), fontSize = 11.sp, color = CC.hintC)
                    }
                    Text("آرگومان: ${c.str("a")}", fontSize = 12.sp, color = CC.label)
                    Text(
                        "لغو",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CC.err,
                        modifier = Modifier
                            .clickable {
                                scope.launch {
                                    try { AppModel.vps().cancelCmd(AppModel.deviceId, c.str("id")) } catch (_: Exception) { }
                                    reload()
                                }
                            }
                            .padding(top = 6.dp)
                    )
                }
            }
            if (pending.isEmpty()) HintGray("صف خالی است ✔")

            SectionTitleWeb("اخیر (${recent.size})")
            recent.forEach { c ->
                ItemCard {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            c.str("c"),
                            fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CC.text,
                            modifier = Modifier.weight(1f)
                        )
                        Text(tsFmt(c.lng("created_at")), fontSize = 11.sp, color = CC.hintC)
                    }
                    val acked = c.opt("acked_at")
                    if (acked == null || acked.toString() == "null" || acked.toString().isEmpty()) {
                        Text("⏳ در انتظار ACK", fontSize = 12.sp, color = CC.warn)
                    } else {
                        val res = c.opt("result")
                        val ok = if (res is JSONObject) res.optBoolean("ok", true)
                                 else !res.toString().startsWith("error:")
                        Text(
                            if (ok) "✔ اجرا شد" else "✖ $res",
                            fontSize = 12.sp,
                            color = if (ok) CC.ok else CC.err
                        )
                    }
                }
            }
            if (recent.isEmpty()) HintGray("فرمانی ارسال نشده")

            BtnWeb("🔄 به‌روزرسانی") { reload() }
            StatusMsg(msg, false)
            BackLink("← بازگشت") { nav.popBackStack() }
        }
        Spacer(Modifier.height(20.dp))
    }
}
