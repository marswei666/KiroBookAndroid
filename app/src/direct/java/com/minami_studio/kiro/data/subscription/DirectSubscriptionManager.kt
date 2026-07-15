package com.minami_studio.kiro.data.subscription

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DirectSubscriptionManager : SubscriptionManager {

    companion object {
        private const val TAG = "DirectSubMgr"
        private const val PREFS_NAME = "subscription_prefs"
        private const val KEY_TIER = "current_tier"
        private const val KEY_EMAIL = "bound_email"
        private const val KEY_ACTIVE = "is_active"
        private const val KEY_EXPIRES = "expires_at"
        private const val KEY_STRIPE_CID = "stripe_customer_id"
        private const val KEY_STRIPE_SID = "stripe_subscription_id"
    }

    private val _state = MutableStateFlow(SubscriptionState())
    override val subscriptionState: StateFlow<SubscriptionState> = _state.asStateFlow()

    private val api = SubscriptionApiService()
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var appContext: Context

    override fun initialize(context: Context) {
        appContext = context.applicationContext
        loadLocalState()
        scope.launch { refreshFromServer() }
    }

    private fun loadLocalState() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tierId = prefs.getString(KEY_TIER, "free") ?: "free"
        val tier = SubscriptionTier.entries.firstOrNull { it.id == tierId } ?: SubscriptionTier.FREE
        _state.value = SubscriptionState(
            tier = tier,
            isActive = prefs.getBoolean(KEY_ACTIVE, false),
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            expiresAt = prefs.getString(KEY_EXPIRES, "") ?: "",
            stripeCustomerId = prefs.getString(KEY_STRIPE_CID, "") ?: "",
            stripeSubscriptionId = prefs.getString(KEY_STRIPE_SID, "") ?: ""
        )
    }

    private fun saveLocalState(state: SubscriptionState) {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_TIER, state.tier.id)
            putBoolean(KEY_ACTIVE, state.isActive)
            putString(KEY_EMAIL, state.email)
            putString(KEY_EXPIRES, state.expiresAt)
            putString(KEY_STRIPE_CID, state.stripeCustomerId)
            putString(KEY_STRIPE_SID, state.stripeSubscriptionId)
            apply()
        }
    }

    private suspend fun refreshFromServer() {
        try {
            val cloudSync = com.minami_studio.kiro.data.sync.CloudSyncService(appContext)
            val uuid = cloudSync.userUUID
            val resp = api.checkSubscription(uuid)
            if (resp.success) {
                val tier = SubscriptionTier.entries.firstOrNull { it.id == resp.tier }
                    ?: SubscriptionTier.FREE
                val newState = SubscriptionState(
                    tier = tier,
                    isActive = resp.isActive,
                    email = resp.email,
                    expiresAt = resp.expiresAt,
                    stripeCustomerId = resp.stripeCustomerId,
                    stripeSubscriptionId = resp.stripeSubscriptionId
                )
                _state.value = newState
                saveLocalState(newState)
            }
        } catch (e: Exception) {
            Log.e(TAG, "refreshFromServer failed: ${e.message}")
        }
    }

    override fun canAddEntry(currentEntryCount: Int): Boolean {
        val state = _state.value
        if (state.isFree) return currentEntryCount < SubscriptionTier.FREE.maxEntries
        if (!state.isActive) return currentEntryCount < SubscriptionTier.FREE.maxEntries
        return currentEntryCount < state.tier.maxEntries
    }

    override fun requiredTierForEntryCount(entryCount: Int): SubscriptionTier {
        return SubscriptionTier.requiredTier(entryCount)
    }

    override suspend fun startPurchase(activity: Activity, tier: SubscriptionTier): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val cloudSync = com.minami_studio.kiro.data.sync.CloudSyncService(appContext)
                val uuid = cloudSync.userUUID
                Log.d(TAG, "startPurchase: uuid=$uuid, tier=${tier.id}")
                
                // 调用云函数创建 Checkout Session
                val response = api.createCheckoutSession(uuid, tier.id)
                Log.d(TAG, "startPurchase: response.success=${response.success}, url=${response.url.take(50)}")
                
                if (response.success && response.url.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "startPurchase: opening browser")
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(response.url)))
                    }
                    Result.success(Unit)
                } else {
                    Log.e(TAG, "startPurchase: failed - ${response.message}")
                    Result.failure(Exception(response.message))
                }
            } catch (e: Exception) {
                Log.e(TAG, "startPurchase failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun restorePurchases(): Result<SubscriptionState> {
        return withContext(Dispatchers.IO) {
            try {
                refreshFromServer()
                Result.success(_state.value)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun openManagementPortal(activity: Activity): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val cloudSync = com.minami_studio.kiro.data.sync.CloudSyncService(appContext)
                val uuid = cloudSync.userUUID
                val portalUrl = api.createPortalSession(uuid)
                if (portalUrl != null) {
                    withContext(Dispatchers.Main) {
                        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(portalUrl)))
                    }
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to create portal session"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override fun isChannelDirect(): Boolean = true
    override fun isChannelPlay(): Boolean = false

    override suspend fun sendVerificationCode(email: String, language: String): SendCodeResponse {
        val cloudSync = com.minami_studio.kiro.data.sync.CloudSyncService(appContext)
        return api.sendVerificationCode(email, cloudSync.userUUID, language)
    }

    override suspend fun verifyAndBind(email: String, code: String): Result<SubscriptionState> {
        return withContext(Dispatchers.IO) {
            try {
                val cloudSync = com.minami_studio.kiro.data.sync.CloudSyncService(appContext)
                val uuid = cloudSync.userUUID
                val resp = api.verifyAndBind(email, code, uuid)
                if (resp.success) {
                    refreshFromServer()
                    Result.success(_state.value)
                } else {
                    Result.failure(Exception(resp.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun unbindDevice(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val cloudSync = com.minami_studio.kiro.data.sync.CloudSyncService(appContext)
                val uuid = cloudSync.userUUID
                val resp = api.unbindDevice(uuid)
                if (resp.success) {
                    val newState = SubscriptionState()
                    _state.value = newState
                    saveLocalState(newState)
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(resp.message))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun forceUnbindByEmail(email: String): SendCodeResponse {
        return api.forceUnbindByEmail(email)
    }
}
