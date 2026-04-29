package com.minami_studio.kiro.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object TranslationService {

    var apiKey: String = ""

    private val client = OkHttpClient()

    private val langToGoogle = mapOf(
        "zh-Hans" to "zh-CN",
        "zh-Hant" to "zh-TW",
        "en"      to "en",
        "ja"      to "ja",
        "ko"      to "ko"
    )

    suspend fun translateToAllLanguages(
        text: String,
        sourceLangCode: String
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val base = mapOf(sourceLangCode to text)
        if (apiKey.isEmpty()) return@withContext base

        val sourceGoogle = langToGoogle[sourceLangCode] ?: return@withContext base
        val targets = langToGoogle.keys.filter { it != sourceLangCode }

        val deferred = targets.map { langCode ->
            async {
                val googleCode = langToGoogle[langCode] ?: return@async null
                val translated = translate(text, sourceGoogle, googleCode)
                if (translated != null) langCode to translated else null
            }
        }

        val results = base.toMutableMap()
        deferred.awaitAll().filterNotNull().forEach { (k, v) -> results[k] = v }
        results
    }

    private fun translate(text: String, from: String, to: String): String? {
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
            null
        }
    }
}
