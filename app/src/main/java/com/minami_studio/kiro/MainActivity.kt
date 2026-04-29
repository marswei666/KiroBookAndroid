package com.minami_studio.kiro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.minami_studio.kiro.ui.theme.WanderLogTheme
import com.minami_studio.kiro.navigation.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 状态栏图标深色（与 iOS 一致）
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        setContent {
            WanderLogTheme {
                AppNavigation()
            }
        }
    }
}
