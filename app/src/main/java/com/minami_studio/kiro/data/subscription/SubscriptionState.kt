package com.minami_studio.kiro.data.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionState(
    val tier: SubscriptionTier = SubscriptionTier.FREE,
    val isActive: Boolean = false,
    val email: String = "",
    val expiresAt: String = "",
    val stripeCustomerId: String = "",
    val stripeSubscriptionId: String = ""
) {
    val isFree: Boolean get() = tier == SubscriptionTier.FREE
    val isPaid: Boolean get() = tier != SubscriptionTier.FREE && isActive
}
