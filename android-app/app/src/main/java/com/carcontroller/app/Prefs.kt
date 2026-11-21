package com.carcontroller.app

// =====================================================================
// Prefs.kt — ذخیرهٔ تنظیمات اتصال (SharedPreferences)
// =====================================================================

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val NAME = "ccapp"
    lateinit var sp: SharedPreferences

    fun init(ctx: Context) {
        sp = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    /** "" یعنی هنوز تنظیم نشده | "vps" | "ap" */
    var mode: String
        get() = sp.getString("mode", "") ?: ""
        set(v) = sp.edit().putString("mode", v).apply()

    /** نشانی کامل داشبورد/REST سرور، مثل https://185.10.20.30:8443 */
    var vpsUrl: String
        get() = sp.getString("vpsUrl", "") ?: ""
        set(v) = sp.edit().putString("vpsUrl", v).apply()

    /** نشانی دستگاه در حالت AP، مثل http://192.168.4.1 */
    var apHost: String
        get() = sp.getString("apHost", "") ?: ""
        set(v) = sp.edit().putString("apHost", v).apply()

    var token: String
        get() = sp.getString("token", "") ?: ""
        set(v) = sp.edit().putString("token", v).apply()

    var phone: String
        get() = sp.getString("phone", "") ?: ""
        set(v) = sp.edit().putString("phone", v).apply()

    var userName: String
        get() = sp.getString("userName", "") ?: ""
        set(v) = sp.edit().putString("userName", v).apply()

    var deviceId: String
        get() = sp.getString("deviceId", "") ?: ""
        set(v) = sp.edit().putString("deviceId", v).apply()

    var deviceName: String
        get() = sp.getString("deviceName", "") ?: ""
        set(v) = sp.edit().putString("deviceName", v).apply()
}
