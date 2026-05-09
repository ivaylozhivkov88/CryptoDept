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

# --- GSON / MOSHI (Data Classes) ---
# Keep all data models used for JSON serialization
-keep class com.cryptodept.data.api.model.** { *; }
-keep class com.cryptodept.domain.model.** { *; }
-keep enum com.cryptodept.domain.model.** { *; }
-keepclassmembers enum com.cryptodept.domain.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# --- ROOM ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# --- HILT ---
-keep class dagger.hilt.** { *; }
-keep class com.cryptodept.di.** { *; }

# --- JETPACK COMPOSE ---
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# --- SQLCIPHER / SQLITE ---
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# --- FIREBASE ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- BUILD CONFIG ---
-keep class com.cryptodept.BuildConfig { *; }
