package com.minami_studio.kiro.util

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object RegionDetector {

    @Volatile
    private var cached: Boolean? = null

    fun isChina(context: Context): Boolean {
        cached?.let { return it }
        val result = detectChina(context)
        cached = result
        return result
    }

    fun clearCache() {
        cached = null
    }

    private fun detectChina(context: Context): Boolean {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        // 1. 检查所有 SIM 卡
        var hasSimInfo = false
        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            try {
                val infos = sm?.activeSubscriptionInfoList
                if (infos != null && infos.isNotEmpty()) {
                    hasSimInfo = true
                    for (info in infos) {
                        if (info.countryIso?.lowercase() == "cn") return true
                    }
                }
            } catch (_: SecurityException) {}
        }

        // 2. 默认 SIM 国家码
        val simCountry = tm?.simCountryIso?.lowercase()
        if (!simCountry.isNullOrEmpty()) {
            hasSimInfo = true
            if (simCountry == "cn") return true
        }

        // 3. 有 SIM 信息且非中国 → 直接返回
        if (hasSimInfo) return false

        // 4. 无 SIM 卡（纯 WiFi）→ 用 IP 地理定位
        try {
            val conn = URL("http://ip-api.com/json/?fields=countryCode").openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val countryCode = org.json.JSONObject(response).optString("countryCode", "").uppercase()
            if (countryCode.isNotEmpty()) {
                return countryCode == "CN"
            }
        } catch (_: Exception) {}

        // 5. 最后兜底：系统语言
        return Locale.getDefault().country.equals("CN", ignoreCase = true)
    }
}
