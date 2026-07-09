package com.minami_studio.kiro.data.sync

import android.content.Context
import android.os.Build
import android.util.Log
import com.minami_studio.kiro.data.model.Entry
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

@Serializable
data class UserStats(
    val userUUID: String,
    val userName: String,
    val totalCheckIns: Int,
    val totalCountries: Int,
    val totalCities: Int,
    val updatedAt: String,
    val platform: String,
    val deviceBrand: String = ""
)

class CloudSyncService(private val context: Context) {

    companion object {
        private const val TAG = "CloudSyncService"
        private const val PREFS_NAME = "cloud_sync_prefs"
        private const val KEY_UUID = "user_uuid"
        private const val ENDPOINT =
            "https://wanderlog-stats-d4fnpamqed206c68-1445354193.ap-shanghai.app.tcloudbase.com/syncUserStats"
    }

    private val json = Json { encodeDefaults = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val userUUID: String
        get() {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val existing = prefs.getString(KEY_UUID, null)
            if (existing != null) {
                Log.d(TAG, "UUID from prefs: $existing")
                return existing
            }
            val newUUID = UUID.randomUUID().toString()
            Log.d(TAG, "Generated new UUID: $newUUID")
            prefs.edit().putString(KEY_UUID, newUUID).apply()
            return newUUID
        }

    private fun brandName(): String {
        val brand = Build.BRAND.lowercase()
        return when {
            brand.contains("xiaomi") || brand.contains("redmi") -> "小米"
            brand.contains("huawei") || brand.contains("honor") -> "华为"
            brand.contains("oppo") -> "OPPO"
            brand.contains("vivo") -> "vivo"
            brand.contains("samsung") -> "三星"
            brand.contains("oneplus") -> "一加"
            brand.contains("meizu") -> "魅族"
            brand.contains("zte") -> "中兴"
            brand.contains("lenovo") -> "联想"
            brand.contains("realme") -> "realme"
            brand.contains("iqoo") -> "iQOO"
            brand.contains("nothing") -> "Nothing"
            brand.contains("pixel") || brand.contains("google") -> "Google"
            else -> Build.BRAND
        }
    }

    fun syncStats(entries: List<Entry>, userName: String) {
        if (ENDPOINT.isEmpty()) {
            Log.d(TAG, "endpoint not configured, skipping")
            return
        }

        val countries = entries.map { it.country }.filter { it.isNotEmpty() }.toSet()
        val cities = entries.filter { it.city.isNotEmpty() }.map { "${it.city},${it.country}" }.toSet()

        val stats = UserStats(
            userUUID = userUUID,
            userName = userName,
            totalCheckIns = entries.size,
            totalCountries = countries.size,
            totalCities = cities.size,
            updatedAt = java.time.Instant.now().toString(),
            platform = "android",
            deviceBrand = brandName()
        )

        Thread {
            try {
                val body = json.encodeToString(stats).toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(ENDPOINT)
                    .post(body)
                    .build()
                val response = client.newCall(request).execute()
                Log.d(TAG, "sync response: ${response.code}")
                response.close()
            } catch (e: Exception) {
                Log.e(TAG, "sync failed: ${e.message}")
            }
        }.start()
    }
}
