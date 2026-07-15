import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

android {
    namespace = "com.minami_studio.kiro"
    compileSdk = 35

    defaultConfig {
        manifestPlaceholders += mapOf()
        applicationId = "com.minami_studio.kiro"
        minSdk = 26
        targetSdk = 35
        versionCode = 16
        versionName = "1.1.2"

        // Google Maps API Key — 从 local.properties 读取
        val mapsApiKey = localProperties.getProperty("MAPS_API_KEY") ?: ""
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        // Google Translate API Key — 从 local.properties 读取
        val translateApiKey = localProperties.getProperty("TRANSLATE_API_KEY") ?: ""
        buildConfigField("String", "TRANSLATE_API_KEY", "\"$translateApiKey\"")

        // AMap API Key — 从 local.properties 读取
        val amapApiKey = localProperties.getProperty("AMAP_API_KEY") ?: ""
        manifestPlaceholders["AMAP_API_KEY"] = amapApiKey
        val amapWebKey = localProperties.getProperty("AMAP_WEB_KEY") ?: ""
        buildConfigField("String", "AMAP_WEB_KEY", "\"$amapWebKey\"")

        // Baidu Translate API Key — 从 local.properties 读取
        val baiduAppId = localProperties.getProperty("BAIDU_TRANSLATE_APPID") ?: ""
        buildConfigField("String", "BAIDU_TRANSLATE_APPID", "\"$baiduAppId\"")
        val baiduSecret = localProperties.getProperty("BAIDU_TRANSLATE_SECRET") ?: ""
        buildConfigField("String", "BAIDU_TRANSLATE_SECRET", "\"$baiduSecret\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("KEYSTORE_PATH") ?: "")
            storePassword = localProperties.getProperty("KEYSTORE_PASSWORD") ?: ""
            keyAlias = localProperties.getProperty("KEY_ALIAS") ?: ""
            keyPassword = localProperties.getProperty("KEY_PASSWORD") ?: ""
            storeType = "PKCS12"
        }
    }

    flavorDimensions += "channel"
    productFlavors {
        create("direct") {
            dimension = "channel"
            // 个人网站下载渠道 —— 包含 Stripe 支付
        }
        create("play") {
            dimension = "channel"
            // Google Play 渠道 —— 包含 Google Play Billing
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)

    implementation(libs.amap3dmap.location.search)

    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.documentfile)

    // Stripe — 仅 direct 渠道
    "directImplementation"("com.stripe:stripe-android:21.5.1")

    // Google Play Billing — 仅 play 渠道
    "playImplementation"("com.android.billingclient:billing-ktx:7.1.1")

    debugImplementation(libs.androidx.ui.tooling)
}
