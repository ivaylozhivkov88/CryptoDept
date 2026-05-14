# CryptoDept ProGuard Rules

# --- GENERAL ---
-keepattributes Signature, Annotation, SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# --- RETROFIT / OKHTTP ---
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-dontwarn retrofit2.**
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep interface com.cryptodept.data.api.** { *; }

# --- GSON / MOSHI (Data Classes) ---
# Keep all data models used for JSON serialization
-keep class com.cryptodept.data.api.model.** { *; }
-keep class com.cryptodept.domain.model.** { *; }
-keep class com.cryptodept.data.db.** { *; }
-keep enum com.cryptodept.domain.model.** { *; }
-keepclassmembers enum com.cryptodept.domain.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- ROOM ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**
-keep interface com.cryptodept.data.db.** { *; }

# --- HILT ---
-keep class dagger.hilt.** { *; }
-keep class com.cryptodept.di.** { *; }
-keep class com.cryptodept.CryptoDeptApplication { *; }

# --- JETPACK COMPOSE ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- GOOGLE GEMINI AI ---
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# --- KOTLIN COROUTINES ---
-keep class kotlinx.coroutines.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepnames class com.cryptodept.** { *; }

# --- SQLCIPHER / SQLITE ---
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# --- FIREBASE ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- BUILD CONFIG ---
-keep class com.cryptodept.BuildConfig { *; }

# --- OPTIMIZATION ---
# Add this to help R8 with memory/processing if needed
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# --- MISSING CLASSES ---
-dontwarn sun.misc.Unsafe
-dontwarn javax.annotation.**

# --- GOOGLE PLAY BILLING ---
-keep class com.android.billingclient.api.** { *; }
-keep class com.android.vending.billing.** { *; }
-dontwarn com.android.billingclient.**

# --- GOOGLE PLAY INTEGRITY (App Check) ---
-keep class com.google.android.play.core.integrity.** { *; }
-dontwarn com.google.android.play.core.**

