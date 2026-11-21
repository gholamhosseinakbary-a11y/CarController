package com.carcontroller.app

// =====================================================================
// Json.kt — کمک‌های JSON (org.json در Android SDK)
// =====================================================================

import org.json.JSONArray
import org.json.JSONObject

/** خطای API با کد HTTP */
class ApiException(val code: Int, message: String) : Exception(message)

fun parseObj(s: String): JSONObject? = try { JSONObject(s) } catch (_: Exception) { null }

fun JSONObject?.str(k: String, def: String = ""): String {
    if (this == null || !has(k) || isNull(k)) return def
    return optString(k, def)
}

fun JSONObject?.bool(k: String, def: Boolean = false): Boolean {
    if (this == null || !has(k) || isNull(k)) return def
    return optBoolean(k, def)
}

fun JSONObject?.int(k: String, def: Int = 0): Int {
    if (this == null || !has(k) || isNull(k)) return def
    return optInt(k, def)
}

fun JSONObject?.lng(k: String, def: Long = 0L): Long {
    if (this == null || !has(k) || isNull(k)) return def
    return optLong(k, def)
}

fun JSONObject?.dbl(k: String, def: Double = 0.0): Double {
    if (this == null || !has(k) || isNull(k)) return def
    return optDouble(k, def)
}

fun JSONObject?.obj(k: String): JSONObject? {
    if (this == null || !has(k) || isNull(k)) return null
    return optJSONObject(k)
}

fun JSONObject?.arr(k: String): JSONArray? {
    if (this == null || !has(k) || isNull(k)) return null
    return optJSONArray(k)
}

fun JSONArray.strs(): List<String> {
    val out = ArrayList<String>()
    for (i in 0 until length()) out.add(optString(i, ""))
    return out
}

fun JSONArray.bools(): List<Boolean> {
    val out = ArrayList<Boolean>()
    for (i in 0 until length()) {
        // [FIX] فریمور «out» را با 0/1 عددی می‌فرستد؛ org.json اعداد را به بولین تبدیل
        // نمی‌کند و optBoolean همیشه false می‌داد → نمایش حالت خروجی‌ها همیشه «خاموش»
        val v = opt(i)
        out.add(when (v) {
            is Boolean -> v
            is Int -> v != 0
            is Long -> v != 0L
            is Double -> v != 0.0
            is String -> v.equals("true", ignoreCase = true) || v == "1"
            else -> false
        })
    }
    return out
}

fun JSONArray.objs(): List<JSONObject> {
    val out = ArrayList<JSONObject>()
    for (i in 0 until length()) {
        val o = optJSONObject(i)
        if (o != null) out.add(o)
    }
    return out
}
