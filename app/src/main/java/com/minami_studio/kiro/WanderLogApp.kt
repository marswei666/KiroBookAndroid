package com.minami_studio.kiro

import android.app.Application
import com.minami_studio.kiro.util.TranslationService

class WanderLogApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TranslationService.apiKey = BuildConfig.TRANSLATE_API_KEY
    }
}
