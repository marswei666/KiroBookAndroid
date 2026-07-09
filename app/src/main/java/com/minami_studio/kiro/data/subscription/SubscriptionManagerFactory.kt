package com.minami_studio.kiro.data.subscription

import android.content.Context
import com.minami_studio.kiro.BuildConfig

object SubscriptionManagerFactory {
    fun create(): SubscriptionManager {
        return when (BuildConfig.FLAVOR) {
            "direct" -> DirectSubscriptionManager()
            "play" -> createPlayManager()
            else -> throw IllegalStateException("Unknown flavor: ${BuildConfig.FLAVOR}")
        }
    }
    
    private fun createPlayManager(): SubscriptionManager {
        // 使用反射创建 PlaySubscriptionManager，避免 direct 编译时找不到类
        return try {
            val clazz = Class.forName("com.minami_studio.kiro.data.subscription.PlaySubscriptionManager")
            clazz.getDeclaredConstructor().newInstance() as SubscriptionManager
        } catch (e: Exception) {
            throw IllegalStateException("PlaySubscriptionManager not found", e)
        }
    }
}
