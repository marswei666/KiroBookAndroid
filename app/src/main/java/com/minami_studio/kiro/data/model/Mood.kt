package com.minami_studio.kiro.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
enum class Mood(val label: String) {
    loved("很爱"),
    relaxed("治愈"),
    amazed("震撼"),
    neutral("一般"),
    tired("疲惫");

    val materialIcon: ImageVector
        get() = when (this) {
            loved -> Icons.Default.Favorite
            relaxed -> Icons.Default.Spa
            amazed -> Icons.Default.Star
            neutral -> Icons.Default.RemoveCircleOutline
            tired -> Icons.Default.NightsStay
        }

    fun localizedLabel(langCode: String): String = when (langCode) {
        "zh-Hans" -> label
        "en" -> when (this) {
            loved -> "Loved"; relaxed -> "Relaxed"; amazed -> "Amazed"
            neutral -> "Neutral"; tired -> "Tired"
        }
        "ja" -> when (this) {
            loved -> "大好き"; relaxed -> "癒し"; amazed -> "感動"
            neutral -> "普通"; tired -> "疲れた"
        }
        "ko" -> when (this) {
            loved -> "사랑해"; relaxed -> "힐링"; amazed -> "감동"
            neutral -> "보통"; tired -> "피곤"
        }
        "zh-Hant" -> when (this) {
            loved -> "很愛"; relaxed -> "療癒"; amazed -> "震撼"
            neutral -> "一般"; tired -> "疲憊"
        }
        else -> label
    }
}
