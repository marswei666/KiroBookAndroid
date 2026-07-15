package com.minami_studio.kiro.data.subscription

import android.util.Log
import com.minami_studio.kiro.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable data class CheckSubscriptionRequest(val userUUID: String, val mode: String = "test")
@Serializable data class SendCodeRequest(val email: String, val userUUID: String, val mode: String = "test", val language: String = "en")
@Serializable data class VerifyBindRequest(val email: String, val code: String, val userUUID: String, val mode: String = "test")
@Serializable data class UnbindDeviceRequest(val userUUID: String, val mode: String = "test")
@Serializable data class ForceUnbindRequest(val email: String, val mode: String = "test")
@Serializable data class PortalSessionRequest(val userUUID: String, val mode: String = "test")
@Serializable data class CheckoutSessionRequest(val userUUID: String, val tier: String, val mode: String = "test")

@Serializable data class CheckSubscriptionResponse(val success: Boolean = false, val tier: String = "free", val isActive: Boolean = false, val email: String = "", val expiresAt: String = "", val stripeCustomerId: String = "", val stripeSubscriptionId: String = "", val message: String = "", val mode: String = "test")
@Serializable data class SendCodeResponse(val success: Boolean = false, val message: String = "")
@Serializable data class VerifyBindResponse(val success: Boolean = false, val message: String = "", val tier: String = "free")
@Serializable data class UnbindResponse(val success: Boolean = false, val message: String = "")
@Serializable data class PortalSessionResponse(val success: Boolean = false, val url: String = "", val message: String = "")
@Serializable data class CheckoutSessionResponse(val success: Boolean = false, val url: String = "", val sessionId: String = "", val message: String = "")
@Serializable data class RecordTransactionRequest(val userUUID: String, val source: String, val eventType: String, val tier: String = "free", val amount: Int = 0, val currency: String = "usd", val transactionId: String = "", val email: String = "", val mode: String = "")
@Serializable data class RecordTransactionResponse(val success: Boolean = false, val message: String = "")

class SubscriptionApiException(val errorCode: Int = -1, override val message: String) : Exception(message)

class SubscriptionApiService {
    companion object {
        private const val TAG = "SubscriptionApi"
        private const val BASE_URL = "https://wanderlog-stats-d4fnpamqed206c68-1445354193.ap-shanghai.app.tcloudbase.com"
        private const val DEFAULT_TIMEOUT = 8L
        private const val CHECKOUT_TIMEOUT = 3L
        private val MODE = if (BuildConfig.DEBUG) "test" else "live"
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true }
    private val client = OkHttpClient.Builder().connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS).writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS).readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS).build()
    private val checkoutClient = OkHttpClient.Builder().connectTimeout(CHECKOUT_TIMEOUT, TimeUnit.SECONDS).writeTimeout(CHECKOUT_TIMEOUT, TimeUnit.SECONDS).readTimeout(CHECKOUT_TIMEOUT, TimeUnit.SECONDS).build()
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun checkSubscription(uuid: String): CheckSubscriptionResponse {
        return post("/checkSubscription", json.encodeToString(CheckSubscriptionRequest(uuid, MODE)))
    }

    suspend fun sendVerificationCode(email: String, uuid: String, language: String = "en"): SendCodeResponse {
        return post("/sendVerificationCode", json.encodeToString(SendCodeRequest(email, uuid, MODE, language)))
    }

    suspend fun verifyAndBind(email: String, code: String, uuid: String): VerifyBindResponse {
        return post("/verifyAndBind", json.encodeToString(VerifyBindRequest(email, code, uuid, MODE)))
    }

    suspend fun unbindDevice(uuid: String): UnbindResponse {
        return post("/unbindDevice", json.encodeToString(UnbindDeviceRequest(uuid, MODE)))
    }

    suspend fun forceUnbindByEmail(email: String): SendCodeResponse {
        return post("/forceUnbindByEmail", json.encodeToString(ForceUnbindRequest(email, MODE)))
    }

    suspend fun createPortalSession(uuid: String): String? {
        return try {
            val response: PortalSessionResponse = post("/createPortalSession", json.encodeToString(PortalSessionRequest(uuid, MODE)))
            if (response.success && response.url.isNotEmpty()) response.url else null
        } catch (e: Exception) { Log.e(TAG, "createPortalSession failed: ${e.message}"); null }
    }

    suspend fun createCheckoutSession(uuid: String, tier: String): CheckoutSessionResponse {
        return postWithClient("/createCheckoutSession", json.encodeToString(CheckoutSessionRequest(uuid, tier, MODE)), checkoutClient)
    }

    suspend fun recordTransaction(uuid: String, source: String, eventType: String, tier: String = "free", amount: Int = 0, currency: String = "usd", transactionId: String = "", email: String = ""): RecordTransactionResponse {
        return post("/recordTransaction", json.encodeToString(RecordTransactionRequest(uuid, source, eventType, tier, amount, currency, transactionId, email, MODE)))
    }

    private suspend inline fun <reified T> post(path: String, bodyJson: String): T {
        return postWithClient(path, bodyJson, client)
    }

    private suspend inline fun <reified T> postWithClient(path: String, bodyJson: String, httpClient: OkHttpClient): T {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = bodyJson.toRequestBody(JSON_MEDIA_TYPE)
                val request = Request.Builder().url("$BASE_URL$path").post(requestBody).header("Content-Type", "application/json").build()
                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                if (!response.isSuccessful) throw SubscriptionApiException(errorCode = response.code, message = "HTTP ${response.code}: $responseBody")
                json.decodeFromString<T>(responseBody)
            } catch (e: SubscriptionApiException) { throw e }
            catch (e: Exception) { Log.e(TAG, "$path failed: ${e.message}"); throw SubscriptionApiException(message = e.message ?: "Unknown error") }
        }
    }
}
