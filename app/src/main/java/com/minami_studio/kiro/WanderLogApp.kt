package com.minami_studio.kiro

import android.app.Application
import com.amap.api.maps.MapsInitializer
import com.minami_studio.kiro.util.TranslationService

class WanderLogApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapsInitializer.updatePrivacyShow(this, true, true)
        MapsInitializer.updatePrivacyAgree(this, true)
        TranslationService.apiKey = BuildConfig.TRANSLATE_API_KEY
        TranslationService.baiduAppId = BuildConfig.BAIDU_TRANSLATE_APPID
        TranslationService.baiduSecret = BuildConfig.BAIDU_TRANSLATE_SECRET
        com.minami_studio.kiro.data.repository.LocationRepository.amapWebKey = BuildConfig.AMAP_WEB_KEY
    }
}
