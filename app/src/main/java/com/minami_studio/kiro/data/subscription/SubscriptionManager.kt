package com.minami_studio.kiro.data.subscription

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

interface SubscriptionManager {

    val subscriptionState: StateFlow<SubscriptionState>

    fun initialize(context: Context)

    fun canAddEntry(currentEntryCount: Int): Boolean

    fun requiredTierForEntryCount(entryCount: Int): SubscriptionTier

    suspend fun startPurchase(activity: Activity, tier: SubscriptionTier): Result<Unit>

    suspend fun restorePurchases(): Result<SubscriptionState>

    suspend fun openManagementPortal(activity: Activity): Result<Unit>

    fun isChannelDirect(): Boolean

    fun isChannelPlay(): Boolean

    suspend fun sendVerificationCode(email: String): SendCodeResponse

    suspend fun verifyAndBind(email: String, code: String): Result<SubscriptionState>

    suspend fun unbindDevice(): Result<Unit>

    suspend fun forceUnbindByEmail(email: String): SendCodeResponse
}
