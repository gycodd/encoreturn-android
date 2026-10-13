package com.encorevpn.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat

/** Только хост для UI: весь интерфейс в App.kt / Home.kt / Logs.kt (Jetpack Compose), логика VPN по-прежнему в Vpn.kt. */
class MainActivity : ComponentActivity() {

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == RESULT_OK) Model.connect(applicationContext) else Model.permissionDenied()
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        Model.init(applicationContext)

        // как на iOS: тёмный фон под системными панелями
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        window.setBackgroundDrawable(ColorDrawable(0xFF0D0B1A.toInt()))
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            EncoreApp(
                onPower = ::onPower,
                onOpenCaptcha = { startActivity(Intent(this, CaptchaActivity::class.java)) },
            )
        }
    }

    private fun onPower() {
        if (Model.phase != Phase.Idle) return
        if (Model.up) { Model.disconnect(); return }
        if (!Model.canConnect()) return
        val i = VpnService.prepare(this)
        if (i != null) vpnPermission.launch(i) else Model.connect(applicationContext)
    }
}
