# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.minami_studio.kiro.**$$serializer { *; }
-keepclassmembers class com.minami_studio.kiro.** { *** Companion; }
-keepclasseswithmembers class com.minami_studio.kiro.** { kotlinx.serialization.KSerializer serializer(...); }

# AMap SDK
-keep class com.amap.api.** { *; }
-keep class com.autonavi.** { *; }
-dontwarn com.amap.api.**
-dontwarn com.autonavi.**
-dontwarn com.amap.ams.**

# Stripe
-keep class com.stripe.android.** { *; }
-dontwarn com.stripe.android.**

# Google Play Billing
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Keep logging for debugging
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
