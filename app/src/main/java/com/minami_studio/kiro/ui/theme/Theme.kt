package com.minami_studio.kiro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WanderLightColorScheme = lightColorScheme(
    primary = WanderAccent,
    onPrimary = Color.White,
    primaryContainer = WanderBlush,
    onPrimaryContainer = WanderInk,
    secondary = WanderMuted,
    onSecondary = Color.White,
    secondaryContainer = WanderBlush,
    onSecondaryContainer = WanderInk,
    tertiary = WanderAccent,
    onTertiary = Color.White,
    background = WanderWarm,
    onBackground = WanderInk,
    surface = WanderCream,
    onSurface = WanderInk,
    surfaceVariant = WanderBlush,
    onSurfaceVariant = WanderMuted,
    surfaceContainerLowest = WanderWarm,
    surfaceContainerLow = WanderWarm,
    surfaceContainer = WanderWarm,
    surfaceContainerHigh = WanderWarm,
    surfaceContainerHighest = WanderWarm,
    surfaceBright = WanderWarm,
    surfaceDim = WanderCream,
    outline = WanderBlush,
    outlineVariant = WanderBlush,
)

@Composable
fun WanderLogTheme(content: @Composable () -> Unit) {
    // 强制浅色模式，与 iOS 版一致
    MaterialTheme(
        colorScheme = WanderLightColorScheme,
        typography = WanderTypography,
        content = content,
    )
}
