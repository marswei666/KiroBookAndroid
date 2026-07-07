package com.minami_studio.kiro.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.minami_studio.kiro.util.RegionDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume

data class GeoResult(
    val latitude: Double,
    val longitude: Double,
    val city: String,
    val country: String
)

object LocationRepository {

    private const val TAG = "LocationRepository"

    var amapWebKey: String = ""

    private const val PI = 3.1415926535897932384626
    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323

    private fun outOfChina(lat: Double, lng: Double): Boolean {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271
    }

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320.0 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLng(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }

    internal fun wgs84ToGcj02(lat: Double, lng: Double): Pair<Double, Double> {
        if (outOfChina(lat, lng)) return Pair(lat, lng)
        var dLat = transformLat(lng - 105.0, lat - 35.0)
        var dLng = transformLng(lng - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * PI
        var magic = Math.sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = Math.sqrt(magic)
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI)
        return Pair(lat + dLat, lng + dLng)
    }

    /** AppLanguage.code → Google API language 参数 */
    private fun googleLangCode(appLangCode: String): String = when (appLangCode) {
        "zh-Hans" -> "zh-CN"
        "zh-Hant" -> "zh-TW"
        "ja" -> "ja"
        "ko" -> "ko"
        else -> "en"
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        return suspendCancellableCoroutine { cont ->
            val cts = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    cont.resume(location)
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
            cont.invokeOnCancellation { cts.cancel() }
        }
    }

    suspend fun getCurrentLatLng(context: Context): Pair<Double, Double>? {
        val location = getCurrentLocation(context) ?: return null
        return if (RegionDetector.isChina(context)) {
            wgs84ToGcj02(location.latitude, location.longitude)
        } else {
            Pair(location.latitude, location.longitude)
        }
    }

    suspend fun geocode(context: Context, latitude: Double, longitude: Double, langCode: String = "en"): GeoResult? {
        if (RegionDetector.isChina(context)) {
            val apiResult = reverseGeocodeByAmapApi(context, latitude, longitude, langCode)
            if (apiResult != null) return apiResult
        } else {
            val apiResult = reverseGeocodeByGoogleApi(context, latitude, longitude, langCode)
            if (apiResult != null) return apiResult
        }

        return try {
            val locale = Locale(googleLangCode(langCode))
            val geocoder = Geocoder(context, locale)
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            val addr = addresses?.firstOrNull()
            if (addr != null) {
                GeoResult(
                    latitude = latitude,
                    longitude = longitude,
                    city = addr.locality ?: addr.subAdminArea ?: "",
                    country = addr.countryName ?: ""
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "geocode failed", e)
            null
        }
    }

    private suspend fun reverseGeocodeByGoogleApi(context: Context, lat: Double, lng: Double, langCode: String): GeoResult? {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getMapsApiKey(context) ?: return@withContext null
                val lang = googleLangCode(langCode)
                val url = "https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lng&language=$lang&key=$apiKey"
                val response = URL(url).readText()
                val json = JSONObject(response)
                if (json.getString("status") == "OK") {
                    val results = json.getJSONArray("results")
                    if (results.length() > 0) {
                        val components = results.getJSONObject(0).getJSONArray("address_components")
                        var city = ""
                        var country = ""
                        var countryCode = ""
                        var admin2 = ""
                        var admin1 = ""
                        var sublocality = ""
                        for (i in 0 until components.length()) {
                            val comp = components.getJSONObject(i)
                            val types = comp.getJSONArray("types")
                            val name = comp.getString("long_name")
                            for (t in 0 until types.length()) {
                                when (types.getString(t)) {
                                    "locality" -> city = name
                                    "country" -> { country = name; countryCode = comp.getString("short_name") }
                                    "administrative_area_level_2" -> admin2 = name
                                    "administrative_area_level_1" -> admin1 = name
                                    "sublocality_level_1", "sublocality" -> sublocality = name
                                }
                            }
                        }
                        // 日本特殊处理：东京/大阪/京都的 locality 是区级，需用 admin1
                        if (countryCode == "JP" && admin1.isNotEmpty()) {
                            val isWard = city.contains("Ward") || (city.contains("City") && sublocality.isNotEmpty())
                            if (city.isEmpty() || isWard) city = admin1
                        } else if (city.isEmpty()) {
                            city = admin1.ifEmpty { admin2.ifEmpty { sublocality } }
                        }
                        // 翻译城市名
                        if (city.isNotEmpty() && lang != "en" && city.all { it.code < 0x80 }) {
                            try {
                                val stripped = city
                                    .replace(Regex("""\s+(Governorate|Province|State|County|District|Region|Prefecture|Municipality)$"""), "")
                                    .trim()
                                val query = if (stripped.isNotEmpty()) "$stripped, $country" else "$city, $country"
                                val cityEncoded = URLEncoder.encode(query, "UTF-8")
                                val cityUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=$cityEncoded&language=$lang&key=$apiKey"
                                val cityResp = URL(cityUrl).readText()
                                val cityJson = JSONObject(cityResp)
                                if (cityJson.getString("status") == "OK") {
                                    val cityResults = cityJson.getJSONArray("results")
                                    if (cityResults.length() > 0) {
                                        val cityComp = cityResults.getJSONObject(0).getJSONArray("address_components")
                                        var translated = ""
                                        for (j in 0 until cityComp.length()) {
                                            val ct = cityComp.getJSONObject(j).getJSONArray("types")
                                            for (k in 0 until ct.length()) {
                                                if (ct.getString(k) == "locality") { translated = cityComp.getJSONObject(j).getString("long_name"); break }
                                            }
                                            if (translated.isNotEmpty()) break
                                        }
                                        if (translated.isEmpty()) {
                                            for (j in 0 until cityComp.length()) {
                                                val ct = cityComp.getJSONObject(j).getJSONArray("types")
                                                for (k in 0 until ct.length()) {
                                                    if (ct.getString(k) == "administrative_area_level_1") { translated = cityComp.getJSONObject(j).getString("long_name"); break }
                                                }
                                                if (translated.isNotEmpty()) break
                                            }
                                        }
                                        if (translated.isNotEmpty() && translated != city) {
                                            city = translated.replace(Regex("""[都府县]$"""), "")
                                                .let { if (it == "北海道") translated else it }
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }
                        GeoResult(lat, lng, city, country)
                    } else null
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "Reverse geocoding API failed", e)
                null
            }
        }
    }

    suspend fun searchAddress(context: Context, query: String, langCode: String = "en"): GeoResult? {
        val trimmed = query.trim()
        Log.d(TAG, "searchAddress: $trimmed")

        val isChina = RegionDetector.isChina(context)

        // 1. 纯坐标：34.0522,-118.2437
        val coordRegex = Regex("""^(-?\d+\.?\d*)\s*[,，]\s*(-?\d+\.?\d*)$""")
        coordRegex.matchEntire(trimmed)?.let { match ->
            val lat = match.groupValues[1].toDoubleOrNull()
            val lng = match.groupValues[2].toDoubleOrNull()
            if (lat != null && lng != null && lat in -90.0..90.0 && lng in -180.0..180.0) {
                Log.d(TAG, "Parsed as coordinates: $lat,$lng")
                return geocode(context, lat, lng, langCode)
            }
        }

        // 2. Google Maps URL
        if (trimmed.contains("google.com/maps") || trimmed.contains("goo.gl/maps") || trimmed.contains("maps.app.goo.gl")) {
            val atRegex = Regex("""@(-?\d+\.?\d*),(-?\d+\.?\d*)""")
            atRegex.find(trimmed)?.let { match ->
                val lat = match.groupValues[1].toDoubleOrNull()
                val lng = match.groupValues[2].toDoubleOrNull()
                if (lat != null && lng != null) {
                    Log.d(TAG, "Extracted coords from URL @: $lat,$lng")
                    return geocode(context, lat, lng, langCode)
                }
            }
            val dRegex = Regex("""!3d(-?\d+\.?\d*)!4d(-?\d+\.?\d*)""")
            dRegex.find(trimmed)?.let { match ->
                val lat = match.groupValues[1].toDoubleOrNull()
                val lng = match.groupValues[2].toDoubleOrNull()
                if (lat != null && lng != null) {
                    Log.d(TAG, "Extracted coords from URL !3d!4d: $lat,$lng")
                    return geocode(context, lat, lng, langCode)
                }
            }
            val placeRegex = Regex("""/place/([^/@]+)""")
            placeRegex.find(trimmed)?.let { match ->
                val placeName = android.net.Uri.decode(match.groupValues[1]).replace("+", " ")
                Log.d(TAG, "Extracted place from URL: $placeName")
                val result = if (isChina) geocodeByAmapApi(context, placeName, langCode)
                             else geocodeByGoogleApi(context, placeName, langCode)
                if (result != null) return result
            }
            val qRegex = Regex("""[?&]q=([^&]+)""")
            qRegex.find(trimmed)?.let { match ->
                val q = android.net.Uri.decode(match.groupValues[1]).replace("+", " ")
                Log.d(TAG, "Extracted q= from URL: $q")
                val result = if (isChina) geocodeByAmapApi(context, q, langCode)
                             else geocodeByGoogleApi(context, q, langCode)
                if (result != null) return result
            }
        }

        // 3. 普通地址文本
        val cleaned = trimmed
            .replace(Regex("""[，,]?\s*美国$"""), "")
            .replace(Regex("""[，,]?\s*中国$"""), "")
            .replace(Regex("""[，,]?\s*日本$"""), "")
            .replace(Regex("""[，,]?\s*한국$"""), "")
            .replace(Regex("""[，,]?\s*Korea$"""), "")
            .replace(Regex("""[，,]?\s*Japan$"""), "")
            .replace(Regex("""[，,]?\s*USA$"""), "")
            .replace(Regex("""[，,]?\s*United States$"""), "")
            .trim()
        Log.d(TAG, "Cleaned address: $cleaned")

        val apiResult = if (isChina) geocodeByAmapApi(context, cleaned, langCode)
                        else geocodeByGoogleApi(context, cleaned, langCode)
        if (apiResult != null) return apiResult

        return geocodeByName(context, cleaned, langCode)
    }

    private suspend fun geocodeByGoogleApi(context: Context, address: String, langCode: String): GeoResult? {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = getMapsApiKey(context) ?: return@withContext null
                val lang = googleLangCode(langCode)
                val encoded = URLEncoder.encode(address, "UTF-8")
                val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$encoded&language=$lang&key=$apiKey"
                val response = URL(url).readText()
                val json = JSONObject(response)
                val status = json.getString("status")
                Log.d(TAG, "Google Geocoding API status: $status")
                if (status == "OK") {
                    val results = json.getJSONArray("results")
                    if (results.length() > 0) {
                        val loc = results.getJSONObject(0)
                            .getJSONObject("geometry")
                            .getJSONObject("location")
                        val lat = loc.getDouble("lat")
                        val lng = loc.getDouble("lng")
                        val components = results.getJSONObject(0).getJSONArray("address_components")
                        var city = ""
                        var country = ""
                        var countryCode = ""
                        var admin2 = ""
                        var admin1 = ""
                        var sublocality = ""
                        for (i in 0 until components.length()) {
                            val comp = components.getJSONObject(i)
                            val types = comp.getJSONArray("types")
                            val name = comp.getString("long_name")
                            for (t in 0 until types.length()) {
                                when (types.getString(t)) {
                                    "locality" -> city = name
                                    "country" -> { country = name; countryCode = comp.getString("short_name") }
                                    "administrative_area_level_2" -> admin2 = name
                                    "administrative_area_level_1" -> admin1 = name
                                    "sublocality_level_1", "sublocality" -> sublocality = name
                                }
                            }
                        }
                        // 日本特殊处理：东京/大阪/京都的 locality 是区级，需用 admin1
                        if (countryCode == "JP" && admin1.isNotEmpty()) {
                            val isWard = city.contains("Ward") || (city.contains("City") && sublocality.isNotEmpty())
                            if (city.isEmpty() || isWard) city = admin1
                        } else if (city.isEmpty()) {
                            city = admin1.ifEmpty { admin2.ifEmpty { sublocality } }
                        }
                        // 如果城市名不是目标语言（如返回 Seoul 而非 首尔），用城市名单独搜索获取翻译
                        if (city.isNotEmpty() && lang != "en" && city.all { it.code < 0x80 }) {
                            try {
                                // 去掉行政后缀，搜索纯城市名（如 "Cairo Governorate" → "Cairo"）
                                val stripped = city
                                    .replace(Regex("""\s+(Governorate|Province|State|County|District|Region|Prefecture|Municipality|Governorate)$"""), "")
                                    .trim()
                                val query = if (stripped.isNotEmpty()) "$stripped, $country" else "$city, $country"
                                val cityEncoded = URLEncoder.encode(query, "UTF-8")
                                val cityUrl = "https://maps.googleapis.com/maps/api/geocode/json?address=$cityEncoded&language=$lang&key=$apiKey"
                                val cityResp = URL(cityUrl).readText()
                                val cityJson = JSONObject(cityResp)
                                if (cityJson.getString("status") == "OK") {
                                    val cityResults = cityJson.getJSONArray("results")
                                    if (cityResults.length() > 0) {
                                        val cityComp = cityResults.getJSONObject(0).getJSONArray("address_components")
                                        // 优先取 locality（城市级），其次 administrative_area_level_1（省级）
                                        var translated = ""
                                        for (j in 0 until cityComp.length()) {
                                            val ct = cityComp.getJSONObject(j).getJSONArray("types")
                                            for (k in 0 until ct.length()) {
                                                if (ct.getString(k) == "locality") {
                                                    translated = cityComp.getJSONObject(j).getString("long_name")
                                                    break
                                                }
                                            }
                                            if (translated.isNotEmpty()) break
                                        }
                                        if (translated.isEmpty()) {
                                            for (j in 0 until cityComp.length()) {
                                                val ct = cityComp.getJSONObject(j).getJSONArray("types")
                                                for (k in 0 until ct.length()) {
                                                    if (ct.getString(k) == "administrative_area_level_1") {
                                                        translated = cityComp.getJSONObject(j).getString("long_name")
                                                        break
                                                    }
                                                }
                                                if (translated.isNotEmpty()) break
                                            }
                                        }
                                        if (translated.isNotEmpty() && translated != city) {
                                            // 去掉日本行政后缀：东京都→东京，大阪府→大阪，愛知県→愛知
                                            val final = translated
                                                .replace(Regex("""[都府县]$"""), "")
                                                .let { if (it == "北海道") translated else it }
                                            Log.d(TAG, "Translated city: $city -> $final")
                                            city = final
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "City translation failed", e)
                            }
                        }
                        Log.d(TAG, "Google Geocoding result: $lat,$lng city=$city country=$country")
                        return@withContext GeoResult(lat, lng, city, country)
                    }
                }
                null
            } catch (e: Exception) {
                Log.e(TAG, "Google Geocoding API failed", e)
                null
            }
        }
    }

    private suspend fun reverseGeocodeByAmapApi(context: Context, lat: Double, lng: Double, langCode: String): GeoResult? {
        return withContext(Dispatchers.IO) {
            try {
                if (amapWebKey.isEmpty()) return@withContext null
                val (gcjLat, gcjLng) = wgs84ToGcj02(lat, lng)
                val url = "https://restapi.amap.com/v3/geocode/regeo?output=json&location=$gcjLng,$gcjLat&key=$amapWebKey&radius=1000&extensions=base"
                val response = URL(url).readText()
                val json = JSONObject(response)
                if (json.getString("status") == "1") {
                    val regeoCode = json.getJSONObject("regeocode")
                    val addressComponent = regeoCode.getJSONObject("addressComponent")
                    var city = addressComponent.optString("city", "")
                    if (city.isEmpty() || city == "[]") {
                        city = addressComponent.optString("province", "")
                    }
                    val country = addressComponent.optString("country", "中国")
                    GeoResult(lat, lng, city, country)
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "AMap reverse geocoding failed", e)
                null
            }
        }
    }

    private suspend fun geocodeByAmapApi(context: Context, address: String, langCode: String): GeoResult? {
        return withContext(Dispatchers.IO) {
            try {
                if (amapWebKey.isEmpty()) return@withContext null
                val encoded = URLEncoder.encode(address, "UTF-8")
                val url = "https://restapi.amap.com/v3/geocode/geo?output=json&address=$encoded&key=$amapWebKey"
                Log.d(TAG, "AMap geocoding URL: $url")
                val response = URL(url).readText()
                Log.d(TAG, "AMap geocoding response: $response")
                val json = JSONObject(response)
                if (json.getString("status") == "1") {
                    val geocodes = json.getJSONArray("geocodes")
                    if (geocodes.length() > 0) {
                        val geocode = geocodes.getJSONObject(0)
                        val location = geocode.getString("location")
                        val parts = location.split(",")
                        if (parts.size == 2) {
                            val lng = parts[0].toDoubleOrNull() ?: return@withContext null
                            val lat = parts[1].toDoubleOrNull() ?: return@withContext null
                            var city = geocode.optString("city", "")
                            if (city.isEmpty() || city == "[]") {
                                city = geocode.optString("province", "")
                            }
                            val country = geocode.optString("country", "中国")
                            GeoResult(lat, lng, city, country)
                        } else null
                    } else null
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "AMap geocoding failed", e)
                null
            }
        }
    }

    private fun getMapsApiKey(context: Context): String? {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName, android.content.pm.PackageManager.GET_META_DATA
            )
            appInfo.metaData?.getString("com.google.android.geo.API_KEY")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Maps API key", e)
            null
        }
    }

    private suspend fun geocodeByName(context: Context, query: String, langCode: String): GeoResult? {
        return try {
            val locale = Locale(googleLangCode(langCode))
            val geocoder = Geocoder(context, locale)
            val addresses = geocoder.getFromLocationName(query, 1)
            val addr = addresses?.firstOrNull()
            if (addr != null) {
                GeoResult(
                    latitude = addr.latitude,
                    longitude = addr.longitude,
                    city = addr.locality ?: addr.subAdminArea ?: "",
                    country = addr.countryName ?: ""
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "geocodeByName failed", e)
            null
        }
    }
}
