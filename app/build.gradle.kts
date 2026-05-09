import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.perf)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
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

        val localProps =
            Properties().apply {
                val localPropsFile = rootProject.file("local.properties")
                if (localPropsFile.exists()) {
                    localPropsFile.inputStream().use { load(it) }
                }
            }

        fun getSecret(key: String): String = localProps.getProperty(key) ?: ""

        buildConfigField("String", "COINGECKO_API_KEY", "\"${getSecret("COINGECKO_API_KEY")}\"")
        buildConfigField("String", "BINANCE_API_KEY", "\"${getSecret("BINANCE_API_KEY")}\"")
        buildConfigField("String", "ETHERSCAN_API_KEY", "\"${getSecret("ETHERSCAN_API_KEY")}\"")
        buildConfigField("String", "CRYPTOPANIC_API_KEY", "\"${getSecret("CRYPTOPANIC_API_KEY")}\"")
        buildConfigField("String", "COINGLASS_API_KEY", "\"${getSecret("COINGLASS_API_KEY")}\"")
        buildConfigField("String", "ALPHA_VANTAGE_API_KEY", "\"${getSecret("ALPHA_VANTAGE_API_KEY")}\"")
        buildConfigField("String", "COINMARKETCAL_API_KEY", "\"${getSecret("COINMARKETCAL_API_KEY")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${getSecret("GEMINI_API_KEY")}\"")
        buildConfigField("String", "HELIUS_API_KEY", "\"${getSecret("HELIUS_API_KEY")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${getSecret("GOOGLE_WEB_CLIENT_ID")}\"")
    }

    signingConfigs {
        create("release") {
            val localProps =
                Properties().apply {
                    val localPropsFile = rootProject.file("local.properties")
                    if (localPropsFile.exists()) {
                        localPropsFile.inputStream().use { load(it) }
                    }
                }
            storeFile = file(localProps.getProperty("KEYSTORE_PATH") ?: "keystore.jks")
            storePassword = localProps.getProperty("KEYSTORE_PASSWORD")
            keyAlias = localProps.getProperty("KEY_ALIAS")
            keyPassword = localProps.getProperty("KEY_PASSWORD")
        }
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
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
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

ktlint {
    version.set("1.3.1")
    android.set(true)
    ignoreFailures.set(true)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
    }
}

detekt {
    toolVersion = "1.23.6"
    config.setFrom("$rootDir/detekt.yml")
    baseline = file("$rootDir/detekt-baseline.xml")
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = false
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
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
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
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
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.collections.immutable)

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
    implementation(libs.firebase.perf)
    implementation(libs.firebase.config)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)

    // Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Play Billing
    implementation("com.android.billingclient:billing-ktx:6.2.1")

    // Play Review
    implementation(libs.play.review.ktx)

    // Image Loading
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    // Security
    implementation(libs.androidx.security.crypto)
    implementation(libs.sqlcipher)
    implementation(libs.sqlite)
    implementation(libs.rootbeer)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.androidx.benchmark.junit4)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.leakcanary.android)
}
