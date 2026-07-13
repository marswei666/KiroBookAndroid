package com.minami_studio.kiro.data.subscription

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.android.billingclient.api.*
import com.minami_studio.kiro.data.sync.CloudSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    private var billingClient: BillingClient? = null
    private lateinit var appContext: Context
    private val scope = CoroutineScope(Dispatchers.IO)
    private val api = SubscriptionApiService()
    private var isPlayStoreInstall = false

    override fun initialize(context: Context) {
        appContext = context.applicationContext
        loadLocalState()

        val installer = appContext.packageManager.getInstallerPackageName(appContext.packageName)
        isPlayStoreInstall = installer == "com.android.vending"
        Log.d(TAG, "Installer: $installer, isPlayStoreInstall: $isPlayStoreInstall")

        if (isPlayStoreInstall) {
            initBillingClient()
        } else {
            Log.d(TAG, "Sideloaded app — using mock billing for testing")
        }
    }

    private fun initBillingClient() {
        billingClient = BillingClient.newBuilder(appContext)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    scope.launch { queryExistingPurchases() }
                } else {
                    Log.w(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected")
                scope.launch {
                    delay(3000)
                    Log.d(TAG, "Reconnecting billing...")
                    initBillingClient()
                }
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
        val client = billingClient ?: return
        if (!client.isReady) return
        val result = client.queryPurchasesAsync(
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

                        // 恢复购买也记录交易
                        scope.launch {
                            try {
                                val uuid = CloudSyncService(appContext).userUUID
                                api.recordTransaction(
                                    uuid = uuid,
                                    source = "google",
                                    eventType = "purchase",
                                    tier = tier.id,
                                    amount = (tier.priceUsd * 100).toInt(),
                                    currency = "usd",
                                    transactionId = purchase.purchaseToken
                                )
                                Log.d(TAG, "Transaction recorded (restore) for ${tier.id}")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to record transaction (restore): ${e.message}")
                            }
                        }
                        return
                    }
                }
            }
        }
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
            if (!isPlayStoreInstall) {
                mockPurchase(tier)
                return@withContext Result.success(Unit)
            }

            val client = billingClient
            if (client == null || !client.isReady) {
                return@withContext Result.failure(Exception("Billing client not ready"))
            }

            try {
                val productList = listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(tier.playProductId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )

                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(productList)
                    .build()

                val productDetailsResult = client.queryProductDetails(params)
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
                    client.launchBillingFlow(activity, billingFlowParams)
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "startPurchase failed: ${e.message}")
                Result.failure(e)
            }
        }
    }

    private suspend fun mockPurchase(tier: SubscriptionTier) {
        Log.d(TAG, "Mock purchase: ${tier.id}")
        val newState = SubscriptionState(tier = tier, isActive = true)
        _state.value = newState
        saveLocalState(newState)

        try {
            val uuid = CloudSyncService(appContext).userUUID
            api.recordTransaction(
                uuid = uuid,
                source = "google",
                eventType = "purchase",
                tier = tier.id,
                amount = (tier.priceUsd * 100).toInt(),
                currency = "usd",
                transactionId = "mock_${System.currentTimeMillis()}"
            )
            Log.d(TAG, "Mock transaction recorded for ${tier.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Mock recordTransaction failed: ${e.message}")
        }
    }

    override suspend fun restorePurchases(): Result<SubscriptionState> {
        return withContext(Dispatchers.IO) {
            try {
                if (isPlayStoreInstall) {
                    queryExistingPurchases()
                }
                Result.success(_state.value)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun openManagementPortal(activity: Activity): Result<Unit> {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://play.google.com/store/account/subscriptions")
                setPackage("com.android.vending")
            }
            activity.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions"))
                activity.startActivity(intent)
                Result.success(Unit)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    override fun isChannelDirect(): Boolean = false
    override fun isChannelPlay(): Boolean = true

    override suspend fun sendVerificationCode(email: String): SendCodeResponse {
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
                val client = billingClient ?: return
                val acknowledgeParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                client.acknowledgePurchase(acknowledgeParams) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase acknowledged")
                    } else {
                        Log.e(TAG, "Acknowledge failed: ${result.debugMessage}, will auto-refund in 3 days")
                    }
                }
            }
            val tier = extractTierFromPurchase(purchase) ?: SubscriptionTier.FREE
            val newState = SubscriptionState(tier = tier, isActive = true)
            _state.value = newState
            saveLocalState(newState)

            scope.launch {
                try {
                    val uuid = CloudSyncService(appContext).userUUID
                    api.recordTransaction(
                        uuid = uuid,
                        source = "google",
                        eventType = "purchase",
                        tier = tier.id,
                        amount = (tier.priceUsd * 100).toInt(),
                        currency = "usd",
                        transactionId = purchase.purchaseToken
                    )
                    Log.d(TAG, "Transaction recorded for ${tier.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to record transaction: ${e.message}")
                }
            }
        }
    }
}
