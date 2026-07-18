package com.minami_studio.kiro.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest

object TranslationService {

    private const val TAG = "TranslationService"

    var apiKey: String = ""
    var baiduAppId: String = ""
    var baiduSecret: String = ""

    private val client = OkHttpClient()

    private val langToGoogle = mapOf(
        "zh-Hans" to "zh-CN",
        "zh-Hant" to "zh-TW",
        "en"      to "en",
        "es"      to "es",
        "fr"      to "fr",
        "ja"      to "ja",
        "ko"      to "ko",
        "ar"      to "ar"
    )

    private val langToBaidu = mapOf(
        "zh-Hans" to "zh",
        "zh-Hant" to "cht",
        "en"      to "en",
        "es"      to "spa",
        "fr"      to "fra",
        "ja"      to "jp",
        "ko"      to "kor",
        "ar"      to "ara"
    )

    suspend fun translateToAllLanguages(
        text: String,
        sourceLangCode: String,
        isChina: Boolean = false
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val base = mapOf(sourceLangCode to text)

        if (isChina) {
            if (baiduAppId.isEmpty() || baiduSecret.isEmpty()) return@withContext base
            val sourceBaidu = langToBaidu[sourceLangCode] ?: return@withContext base
            val targets = langToBaidu.keys.filter { it != sourceLangCode }

            val deferred = targets.map { langCode ->
                async {
                    val baiduCode = langToBaidu[langCode] ?: return@async null
                    val translated = translateBaidu(text, sourceBaidu, baiduCode)
                    if (translated != null) langCode to translated else null
                }
            }

            val results = base.toMutableMap()
            deferred.awaitAll().filterNotNull().forEach { (k, v) -> results[k] = v }
            results
        } else {
            if (apiKey.isEmpty()) return@withContext base
            val sourceGoogle = langToGoogle[sourceLangCode] ?: return@withContext base
            val targets = langToGoogle.keys.filter { it != sourceLangCode }

            val deferred = targets.map { langCode ->
                async {
                    val googleCode = langToGoogle[langCode] ?: return@async null
                    val translated = translateGoogle(text, sourceGoogle, googleCode)
                    if (translated != null) langCode to translated else null
                }
            }

            val results = base.toMutableMap()
            deferred.awaitAll().filterNotNull().forEach { (k, v) -> results[k] = v }
            results
        }
    }

    private fun translateGoogle(text: String, from: String, to: String): String? {
        return try {
            val body = JSONObject().apply {
                put("q", text)
                put("source", from)
                put("target", to)
                put("format", "text")
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://translation.googleapis.com/language/translate/v2?key=$apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string() ?: return null
            JSONObject(bodyStr)
                .getJSONObject("data")
                .getJSONArray("translations")
                .getJSONObject(0)
                .getString("translatedText")
        } catch (e: Exception) {
            Log.e(TAG, "Google translate failed", e)
            null
        }
    }

    private fun translateBaidu(text: String, from: String, to: String): String? {
        return try {
            val salt = System.currentTimeMillis().toString()
            val sign = md5(baiduAppId + text + salt + baiduSecret)

            val requestBody = "q=${java.net.URLEncoder.encode(text, "UTF-8")}" +
                "&from=$from&to=$to" +
                "&appid=$baiduAppId&salt=$salt&sign=$sign"

            val request = Request.Builder()
                .url("https://fanyi-api.baidu.com/api/trans/vip/translate")
                .post(requestBody.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val bodyStr = response.body?.string() ?: return null
            val json = JSONObject(bodyStr)
            val results = json.getJSONArray("trans_result")
            if (results.length() > 0) {
                results.getJSONObject(0).getString("dst")
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Baidu translate failed", e)
            null
        }
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
