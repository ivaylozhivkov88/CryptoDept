# ============================================================
# CryptoDept R8/ProGuard Rules — v1.1.0
# Philosophy: Minimum invasive. Trust R8 default behavior.
# ============================================================

# ============================================================
# CORE: Application class (entry point)
# ============================================================
-keep class com.cryptodept.CryptoDeptApplication { *; }

# ============================================================
# REFLECTION: Gson serialization
# Gson uses reflection to access fields. R8 will obfuscate
# field names → JSON parsing breaks unless we tell R8 to keep.
# ============================================================

# Generic Gson type adapter
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep generic type information for fields annotated with @SerializedName
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep DTO classes used with Gson (use specific package)
# These are network DTOs that get serialized/deserialized from JSON.
# Without this, R8 obfuscates field names and Gson can't map JSON keys.
-keep class com.cryptodept.data.api.**.dto.** { *; }
-keep class com.cryptodept.data.api.**.*Dto { *; }
-keep class com.cryptodept.data.api.**.*Response { *; }
-keep class com.cryptodept.data.api.**.*Request { *; }

# ============================================================
# RETROFIT: HTTP client
# Retrofit uses reflection on interface methods.
# ============================================================
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Retrofit service interfaces — методите се извикват via reflection
-keep interface com.cryptodept.data.api.** {
    @retrofit2.http.* <methods>;
}

# ============================================================
# OKHTTP: Network layer
# ============================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# ============================================================
# KOTLIN COROUTINES
# ============================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================================================
# KOTLIN SERIALIZATION (if used anywhere)
# ============================================================
-keep,includedescriptorclasses class com.cryptodept.**$$serializer { *; }
-keepclassmembers class com.cryptodept.** {
    *** Companion;
}
-keepclasseswithmembers class com.cryptodept.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ============================================================
# HILT / DAGGER: Dependency injection
# Hilt generates code that accesses methods via reflection.
# ============================================================
-keep class * extends androidx.hilt.work.HiltWorker
-keep class dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }

# Hilt-generated classes
-keep class com.cryptodept.**_HiltModules** { *; }
-keep class com.cryptodept.**_GeneratedInjector { *; }
-keep class com.cryptodept.**.Hilt_** { *; }

# ============================================================
# ROOM: Database
# ============================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep @androidx.room.TypeConverters class * { *; }

# Keep DAO methods (called via reflection)
-keepclassmembers class * {
    @androidx.room.Query <methods>;
    @androidx.room.Insert <methods>;
    @androidx.room.Update <methods>;
    @androidx.room.Delete <methods>;
    @androidx.room.Transaction <methods>;
}

# ============================================================
# JETPACK COMPOSE
# ============================================================
# Compose uses reflection for previews and tooling
-keep class androidx.compose.runtime.Composer { *; }
-keep class androidx.compose.runtime.ComposerKt { *; }

# Keep @Composable annotations
-keepattributes RuntimeVisibleAnnotations
-keep @androidx.compose.runtime.Composable class *
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ============================================================
# FIREBASE
# ============================================================
# Firebase services use reflection
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Firebase Analytics
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Firebase Crashlytics — needed for proper stack traces
-keep class * extends java.lang.Exception
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================
# GOOGLE PLAY BILLING
# ============================================================
-keep class com.android.billingclient.api.** { *; }

# Critical: BillingService и related classes used through callbacks
-keep class com.cryptodept.data.billing.BillingService { *; }
-keep class com.cryptodept.data.billing.BillingService$* { *; }

# ============================================================
# GOOGLE PLAY APP UPDATES (In-App Updates)
# ============================================================
-keep class com.google.android.play.core.** { *; }

# ============================================================
# WORKMANAGER
# ============================================================
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker

# Hilt + WorkManager: keep @HiltWorker constructors
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ============================================================
# CRT THEME / COMPOSE COLORS
# Keep typography sealed classes (used in reflection-based theming)
# ============================================================
-keepclassmembers class com.cryptodept.ui.theme.** {
    public <fields>;
}

# ============================================================
# DOMAIN MODELS USED BY VIEW MODELS
# (StateFlow + collectAsState requires data class structure)
# ============================================================
-keep class com.cryptodept.domain.model.** { *; }

# UI state classes need to be preserved for proper recomposition
-keepclassmembers class com.cryptodept.**UiState { *; }

# ============================================================
# PREDICTION ENGINES (use reflection internally)
# ============================================================
-keep class com.cryptodept.domain.prediction.engine.** { *; }

# ============================================================
# ENUM CLASSES — used in when/serialization
# ============================================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================
# PARCELABLE
# ============================================================
-keep class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ============================================================
# SERIALIZABLE (avoid SerialVersionUID issues)
# ============================================================
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================
# CRT TERMINAL DRAWING / CANVAS
# Custom drawing classes use reflection for animation
# ============================================================
-keep class com.cryptodept.ui.effects.** { *; }

# ============================================================
# AGENT SYSTEM (uses reflection for agent discovery)
# ============================================================
-keep class com.cryptodept.domain.agent.** { *; }
-keep class com.cryptodept.service.agent.** { *; }

# ============================================================
# DEBUG SAFETY: Keep stacktraces for Crashlytics
# ============================================================
-keepattributes SourceFile, LineNumberTable

# ============================================================
# REMOVE LOGGING IN RELEASE
# Strips Log.d, Log.v calls — performance + size improvement
# ============================================================
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# Timber (ако се ползва)
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
}

# ============================================================
# DONTWARN — suppress R8 warnings for known-safe missing classes
# ============================================================
-dontwarn java.lang.invoke.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.lang.model.element.Modifier

# Lottie animations (ако се ползва)
-dontwarn com.airbnb.lottie.**

# Apache HTTP (legacy, may be referenced by some libs)
-dontwarn org.apache.http.**

# ============================================================
# TOTAL STABILITY CONFIGURATION (NO CUTTING, NO RENAMING)
# ============================================================

-dontobfuscate
-dontoptimize
-dontshrink
-ignorewarnings

# Запазване на абсолютно целия проект
-keep class com.cryptodept.** { *; }
-keep interface com.cryptodept.** { *; }

# Запазване на всички библиотеки (бизнес логика и DTO-та)
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
-keep class net.sqlcipher.** { *; }
-keep class net.zetetic.** { *; }
-keep class com.google.firebase.** { *; }
-keep class androidx.room.** { *; }
-keep class androidx.work.** { *; }

# КРИТИЧНО за SQLCipher
-keepclassmembers class net.sqlcipher.database.** {
    private long mNativeHandle;
    <fields>;
}
-keepclassmembers class net.zetetic.database.** {
    private long mNativeHandle;
    <fields>;
}

# JNI методи
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================
# END OF RULES
# ============================================================
