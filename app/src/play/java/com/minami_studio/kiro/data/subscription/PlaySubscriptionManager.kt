package com.minami_studio.kiro.data.subscription

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaySubscriptionManager : SubscriptionManager, PurchasesUpdatedListener {

    companion object {
        private const val TAG = "PlaySubMgr"
        private const val PREFS_NAME = "subscription_prefs"
        private const val KEY_TIER = "current_tier"
        private const val KEY_ACTIVE = "is_active"
    }

    private val _state = MutableStateFlow(SubscriptionState())
    override val subscriptionState: StateFlow<SubscriptionState> = _state.asStateFlow()

    private lateinit var billingClient: BillingClient
    private lateinit var appContext: Context
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun initialize(context: Context) {
        appContext = context.applicationContext
        loadLocalState()

        billingClient = BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    scope.launch { queryExistingPurchases() }
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected")
            }
        })
    }

    private fun loadLocalState() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val tierId = prefs.getString(KEY_TIER, "free") ?: "free"
        val tier = SubscriptionTier.entries.firstOrNull { it.id == tierId } ?: SubscriptionTier.FREE
        _state.value = SubscriptionState(
            tier = tier,
            isActive = prefs.getBoolean(KEY_ACTIVE, false)
        )
    }

    private fun saveLocalState(state: SubscriptionState) {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(KEY_TIER, state.tier.id)
            putBoolean(KEY_ACTIVE, state.isActive)
            apply()
        }
    }

    private suspend fun queryExistingPurchases() {
        if (!billingClient.isReady) return
        val result = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            for (purchase in result.purchasesList) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    val tier = extractTierFromPurchase(purchase)
                    if (tier != null) {
                        val newState = SubscriptionState(tier = tier, isActive = true)
                        _state.value = newState
                        saveLocalState(newState)
                        return
                    }
                }
            }
        }
        // No active subscription
        val newState = SubscriptionState(tier = SubscriptionTier.FREE, isActive = false)
        _state.value = newState
        saveLocalState(newState)
    }

    private fun extractTierFromPurchase(purchase: Purchase): SubscriptionTier? {
        for (productId in purchase.products) {
            val tier = SubscriptionTier.entries.firstOrNull { it.playProductId == productId }
            if (tier != null) return tier
        }
        return null
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
                if (!billingClient.isReady) {
                    return@withContext Result.failure(Exception("Billing client not ready"))
                }

                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(tier.playProductId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )

                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

                val productDetailsResult = billingClient.queryProductDetails(params)
                if (productDetailsResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    return@withContext Result.failure(Exception("Failed to query product details"))
                }

                val productDetails = productDetailsResult.productDetailsList?.firstOrNull()
                    ?: return@withContext Result.failure(Exception("Product not found"))

                val offerDetails = productDetails.subscriptionOfferDetails?.firstOrNull()
                    ?: return@withContext Result.failure(Exception("No offer details"))

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerDetails.offerToken)
                                .build()
                        )
                    )
                    .build()

                withContext(Dispatchers.Main) {
                    billingClient.launchBillingFlow(activity, billingFlowParams)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "startPurchase failed: ${e.message}")
                Result.failure(e)
            }
        }
    }

    override suspend fun restorePurchases(): Result<SubscriptionState> {
        return withContext(Dispatchers.IO) {
            try {
                queryExistingPurchases()
                Result.success(_state.value)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun openManagementPortal(activity: Activity): Result<Unit> {
        // Google Play 没有类似 Stripe Customer Portal 的功能
        // 用户可以通过 Google Play 订阅管理页面取消
        return Result.success(Unit)
    }

    override fun isChannelDirect(): Boolean = false
    override fun isChannelPlay(): Boolean = true

    override suspend fun sendVerificationCode(email: String): SendCodeResponse {
        // Google Play 渠道不需要邮箱验证
        return SendCodeResponse(false, "Not supported on Play channel")
    }

    override suspend fun verifyAndBind(email: String, code: String): Result<SubscriptionState> {
        return Result.failure(Exception("Not supported on Play channel"))
    }

    override suspend fun unbindDevice(): Result<Unit> {
        return Result.failure(Exception("Not supported on Play channel"))
    }

    override suspend fun forceUnbindByEmail(email: String): SendCodeResponse {
        return SendCodeResponse(false, "Not supported on Play channel")
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Purchase cancelled by user")
        } else {
            Log.e(TAG, "Purchase error: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(acknowledgeParams) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged")
                    }
                }
            }
            val tier = extractTierFromPurchase(purchase) ?: SubscriptionTier.FREE
            val newState = SubscriptionState(tier = tier, isActive = true)
            _state.value = newState
            saveLocalState(newState)
        }
    }
}
