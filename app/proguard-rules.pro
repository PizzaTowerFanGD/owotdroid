# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-keep class com.owot.android.client.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep Gson classes
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Room database classes
-keep class androidx.room.** { *; }
-dontwarn androidx.room.paging.**

# Keep Hilt classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Android specific
-keep public class * extends android.app.Activity
-keep public class * extends android.webkit.WebViewClient
-keep public class * extends android.webkit.WebChromeClient
-dontwarn android.webkit.**

# Kotlin specific
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.Metadata