import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.cryptodept"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cryptodept"
        minSdk = 26
        targetSdk = 35
        versionCode = 304
        versionName = "3.0.4-SUPREME"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val secrets = Properties().apply {
            val secretsFile = rootProject.file("secrets.properties")
            if (secretsFile.exists()) {
                load(secretsFile.inputStream())
            }
        }

        buildConfigField("String", "COINGECKO_API_KEY", "\"${secrets["COINGECKO_API_KEY"] ?: ""}\"")
        buildConfigField("String", "BINANCE_API_KEY", "\"${secrets["BINANCE_API_KEY"] ?: ""}\"")
        buildConfigField("String", "ETHERSCAN_API_KEY", "\"${secrets["ETHERSCAN_API_KEY"] ?: ""}\"")
        buildConfigField("String", "CRYPTOPANIC_API_KEY", "\"${secrets["CRYPTOPANIC_API_KEY"] ?: ""}\"")
        buildConfigField("String", "COINGLASS_API_KEY", "\"${secrets["COINGLASS_API_KEY"] ?: ""}\"")
        buildConfigField("String", "ALPHA_VANTAGE_API_KEY", "\"${secrets["ALPHA_VANTAGE_API_KEY"] ?: ""}\"")
        buildConfigField("String", "COINMARKETCAL_API_KEY", "\"${secrets["COINMARKETCAL_API_KEY"] ?: ""}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${secrets["GEMINI_API_KEY"] ?: ""}\"")
        buildConfigField("String", "WHALE_ALERT_API_KEY", "\"${secrets["WHALE_ALERT_API_KEY"] ?: ""}\"")
    }

    // Enable Room schema export for migration tracking
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Charts
    implementation(libs.mpandroidchart)

    // Accompanist
    implementation(libs.accompanist.permissions)

    // Glance (Widgets)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.appcheck)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)

    // Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Play Billing
    implementation("com.android.billingclient:billing-ktx:6.2.1")

    // Play Review
    implementation(libs.play.review.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.mockk.android)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}