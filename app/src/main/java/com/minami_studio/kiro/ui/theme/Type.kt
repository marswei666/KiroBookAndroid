package com.minami_studio.kiro.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 使用系统 Serif 字体（类似 Georgia 的衬线字体）
// 如需精确还原 iOS 效果，可将 Georgia .ttf 文件放入 res/font/ 并取消注释下方代码
val Georgia = FontFamily.Serif

val WanderTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Georgia,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        color = WanderInk,
    ),
    headlineLarge = TextStyle(
        fontFamily = Georgia,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        color = WanderInk,
    ),
    headlineMedium = TextStyle(
        fontFamily = Georgia,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        color = WanderInk,
    ),
    titleLarge = TextStyle(
        fontFamily = Georgia,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        color = WanderInk,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        color = WanderInk,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        color = WanderInk,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        color = WanderInk,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = WanderMuted,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = WanderInk,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        color = WanderInk,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        color = WanderMuted,
    ),
)
