# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.minami_studio.kiro.**$$serializer { *; }
-keepclassmembers class com.minami_studio.kiro.** { *** Companion; }
-keepclasseswithmembers class com.minami_studio.kiro.** { kotlinx.serialization.KSerializer serializer(...); }
