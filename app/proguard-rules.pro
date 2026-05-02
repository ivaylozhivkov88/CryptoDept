# Retrofit + OkHttp
-keepattributes Signature, Exceptions, *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepclasseswithmembers class * { @retrofit2.http.* <methods>; }
-dontwarn okhttp3.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keepclasseswithmembers class * { @javax.inject.Inject *; }

# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Kotlin Serialization / Coroutines
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Domain models — не обфусквай имената (за Room, JSON)
-keep class com.cryptodept.domain.model.** { *; }
-keep class com.cryptodept.data.db.** { *; }
-keep class com.cryptodept.data.api.model.** { *; }

# Gemini API
-keep class com.google.ai.client.generativeai.** { *; }

# Play Review
-keep class com.google.android.play.core.review.** { *; }

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite { *; }
