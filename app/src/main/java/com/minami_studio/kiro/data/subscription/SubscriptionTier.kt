package com.minami_studio.kiro.data.subscription

import com.minami_studio.kiro.BuildConfig
import kotlinx.serialization.Serializable

@Serializable
enum class SubscriptionTier(
    val id: String,
    val maxEntries: Int,
    private val priceDirect: Double,
    private val pricePlay: Double,
    val stripePriceId: String,
    val playProductId: String
) {
    FREE("free", if (BuildConfig.DEBUG) 3 else 30, 0.0, 0.0, "", ""),
    TIER_1("tier_1", if (BuildConfig.DEBUG) 6 else 60, 5.0, 9.99, "price_tier1_monthly", "tier1_monthly"),
    TIER_2("tier_2", if (BuildConfig.DEBUG) 9 else 90, 10.0, 14.99, "price_tier2_monthly", "tier2_monthly"),
    TIER_3("tier_3", Int.MAX_VALUE, 15.0, 19.99, "price_tier3_monthly", "tier3_monthly");

    val priceUsd: Double
        get() = if (BuildConfig.FLAVOR == "play") pricePlay else priceDirect

    val displayName: String
        get() = when (this) {
            FREE -> "Free"
            TIER_1 -> if (BuildConfig.FLAVOR == "play") "$9.99/month" else "$5/month"
            TIER_2 -> if (BuildConfig.FLAVOR == "play") "$14.99/month" else "$10/month"
            TIER_3 -> if (BuildConfig.FLAVOR == "play") "$19.99/month" else "$15/month"
        }

    companion object {
        fun requiredTier(entryCount: Int): SubscriptionTier {
            val freeLimit = if (BuildConfig.DEBUG) 3 else 30
            val tier1Limit = if (BuildConfig.DEBUG) 6 else 60
            val tier2Limit = if (BuildConfig.DEBUG) 9 else 90
            
            return when {
                entryCount <= freeLimit -> FREE
                entryCount <= tier1Limit -> TIER_1
                entryCount <= tier2Limit -> TIER_2
                else -> TIER_3
            }
        }
    }
}
