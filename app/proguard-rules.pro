# CryptoDept - Terminal ProGuard Rules

# General Rules
-keepattributes Signature, Exceptions, *Annotation*, EnclosingMethod, InnerClasses
-keepattributes SourceFile, LineNumberTable

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# Play In-App Review
-keep class com.google.android.play.core.review.** { *; }
-dontwarn com.google.android.play.core.review.**

# Play Billing
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# Glance AppWidget (Jetpack)
-keep class androidx.glance.** { *; }
-dontwarn androidx.glance.**

# WorkManager
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# OkHttp
-keep class okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-keepattributes Signature, Exceptions, *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-dontwarn retrofit2.**

# Gson / JSON
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Domain & Data Models (Critical for serialization/Room)
-keep class com.cryptodept.domain.model.** { *; }
-keep class com.cryptodept.data.db.** { *; }
-keep class com.cryptodept.data.api.model.** { *; }
-keep class com.cryptodept.data.billing.** { *; }
-keep class com.cryptodept.data.datastore.** { *; }

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * { @javax.inject.Inject *; }
-keepclasseswithmembers class * { @dagger.hilt.* *; }

# Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Gemini AI (Google AI SDK)
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keep class * implements androidx.room.TypeConverter

# DataStore / Protobuf
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
