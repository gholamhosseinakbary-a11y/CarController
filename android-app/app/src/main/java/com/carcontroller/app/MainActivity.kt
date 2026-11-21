package com.carcontroller.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.carcontroller.app.ui.CC
import com.carcontroller.app.ui.CCTheme
import com.carcontroller.app.ui.ccGradient
import com.carcontroller.app.ui.screens.ApConnectScreen
import com.carcontroller.app.ui.screens.DevicesScreen
import com.carcontroller.app.ui.screens.EventsScreen
import com.carcontroller.app.ui.screens.HomeScreen
import com.carcontroller.app.ui.screens.LoginScreen
import com.carcontroller.app.ui.screens.QueueScreen
import com.carcontroller.app.ui.screens.SetupScreen
import com.carcontroller.app.ui.screens.ApSettingsScreen
import com.carcontroller.app.ui.screens.GpsSettingsScreen
import com.carcontroller.app.ui.screens.RfidSettingsScreen
import com.carcontroller.app.ui.screens.SettingsMenuScreen
import com.carcontroller.app.ui.screens.SimSettingsScreen
import com.carcontroller.app.ui.screens.VpsSettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(applicationContext)
        AppModel.loadFromPrefs()
        setContent {
            CCTheme {
                // رابط کاملاً فارسی/راست‌به‌چپ
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppNav()
                }
            }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val start = remember {
        when {
            AppModel.mode.isEmpty() -> "setup"
            AppModel.mode == "ap" -> "apconnect"
            Prefs.token.isEmpty() -> "login"
            Prefs.deviceId.isEmpty() -> "devices"
            else -> "home"
        }
    }

    // تازه‌سازی خودکار وضعیت — مثل setInterval جاوااسکریپت پنل وب
    LaunchedEffect(AppModel.mode, Prefs.deviceId) {
        if (AppModel.mode.isNotEmpty()) AppModel.startPolling()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ccGradient())   // پس‌زمینهٔ مشکی (پوستهٔ تاریک UI-v6)
    ) {
        NavHost(navController = nav, startDestination = start) {
            composable("setup") { SetupScreen(nav) }
            composable("login") { LoginScreen(nav) }
            composable("devices") { DevicesScreen(nav) }
            composable("apconnect") { ApConnectScreen(nav) }
            composable("home") { HomeScreen(nav) }
            composable("settings") { SettingsMenuScreen(nav) }
            composable("settings/ap") { ApSettingsScreen(nav) }
            composable("settings/sim") { SimSettingsScreen(nav) }
            composable("settings/gps") { GpsSettingsScreen(nav) }
            composable("settings/rfid") { RfidSettingsScreen(nav) }
            composable("settings/vps") { VpsSettingsScreen(nav) }
            composable("events") { EventsScreen(nav) }
            composable("queue") { QueueScreen(nav) }
        }

        // پیام کوتاه سراسری (نتایج فرمان‌ها) — به سبک نوار پیام پنل
        if (AppModel.toast.isNotEmpty()) {
            Surface(
                color = CC.toastBg,
                contentColor = CC.white,
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(
                    AppModel.toast,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

/** پاک‌کردن پشته و رفتن به مسیر جدید */
fun NavHostController.go(route: String) {
    navigate(route) {
        popUpTo(0) { inclusive = true }
    }
}
