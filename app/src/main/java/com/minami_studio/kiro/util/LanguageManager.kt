package com.minami_studio.kiro.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppLanguage(val code: String, val displayName: String, val locale: java.util.Locale) {
    simplifiedChinese("zh-Hans", "简体中文", java.util.Locale.SIMPLIFIED_CHINESE),
    english("en", "English", java.util.Locale.ENGLISH),
    japanese("ja", "日本語", java.util.Locale.JAPANESE),
    korean("ko", "한국어", java.util.Locale.KOREAN),
    traditionalChinese("zh-Hant", "繁體中文", java.util.Locale.TRADITIONAL_CHINESE);

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: simplifiedChinese
    }
}

class LanguageManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString("appLanguage", "") ?: "")
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    val langCode: String get() = _language.value.code

    val s: Strings get() = Strings(_language.value)

    fun setLanguage(lang: AppLanguage) {
        _language.value = lang
        prefs.edit().putString("appLanguage", lang.code).apply()
    }

    // 获取当前语言的 weekday 缩写
    val weekdayAbbreviations: List<String>
        get() = when (_language.value) {
            AppLanguage.simplifiedChinese, AppLanguage.traditionalChinese ->
                listOf("日", "一", "二", "三", "四", "五", "六")
            AppLanguage.english ->
                listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
            AppLanguage.japanese ->
                listOf("日", "月", "火", "水", "木", "金", "土")
            AppLanguage.korean ->
                listOf("일", "월", "화", "수", "목", "금", "토")
        }
}
