package com.minami_studio.kiro

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.minami_studio.kiro.ui.theme.WanderLogTheme
import com.minami_studio.kiro.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    private var onAppResume: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        handleDeepLink(intent)
        setContent {
            WanderLogTheme {
                AppNavigation(onAppResume = { callback -> onAppResume = callback })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    override fun onResume() {
        super.onResume()
        // 从后台返回或支付完成后刷新订阅状态
        onAppResume?.invoke()
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        Log.d("MainActivity", "Deep link: $data")
        if (data.scheme == "wanderlog" && data.host == "checkout") {
            when (data.path) {
                "/success" -> Log.d("MainActivity", "Checkout success")
                "/cancel" -> Log.d("MainActivity", "Checkout cancelled")
            }
        }
    }
}
