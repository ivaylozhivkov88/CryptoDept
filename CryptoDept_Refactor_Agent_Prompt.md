# CryptoDept Terminal v1.1.1 — God Object Refactoring
## Agent Prompt for Android Studio

---

## CONTEXT

You are working on **CryptoDept Terminal**, an Android crypto intelligence app built with:
- Clean Architecture + Hilt DI
- Kotlin Coroutines + StateFlow
- Jetpack Compose UI
- DataStore + SQLCipher
- Naming convention: `XxxService` suffix for singletons (avoid `Manager`); `XxxRepository` for data; `XxxViewModel` for presentation.

**Your mission:** Execute a safe, zero-breakage architectural refactoring of two God Objects. You must not break any existing DataStore keys, any existing DI bindings, or any existing business logic. You are adding structure ON TOP of what exists, not rewriting internals.

---

## TASK A — SPLIT `PreferencesService` INTO 3 FOCUSED INTERFACES

### A1. CREATE file: `app/src/main/java/com/cryptodept/data/datastore/SystemSettingsManager.kt`

```kotlin
package com.cryptodept.data.datastore

import kotlinx.coroutines.flow.Flow

/**
 * Manages purely visual and audio system configuration.
 * No knowledge of subscriptions, users, or business logic.
 */
interface SystemSettingsManager {
    val refreshInterval: Flow<Int>
    val phosphorMode: Flow<String>
    val soundsEnabled: Flow<Boolean>
    val soundsVolume: Flow<Float>
    val hapticEnabled: Flow<Boolean>
    val notificationsEnabled: Flow<Boolean>
    val screensaverTimeout: Flow<Int>
    val powerUserMode: Flow<Boolean>
    val focusModeEnabled: Flow<Boolean>
    val crashlyticsConsent: Flow<Boolean>
    val forceShowAllFeatures: Flow<Boolean>

    suspend fun setRefreshInterval(seconds: Int)
    suspend fun setPhosphorMode(mode: String)
    suspend fun setSoundsEnabled(enabled: Boolean)
    suspend fun setSoundsVolume(volume: Float)
    suspend fun setHapticEnabled(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setScreensaverTimeout(minutes: Int)
    suspend fun setPowerUserMode(enabled: Boolean)
    suspend fun setFocusModeEnabled(enabled: Boolean)
    suspend fun setCrashlyticsConsent(enabled: Boolean)
    suspend fun setForceShowAllFeatures(enabled: Boolean)
}
```

---

### A2. CREATE file: `app/src/main/java/com/cryptodept/data/datastore/UserSessionManager.kt`

```kotlin
package com.cryptodept.data.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages onboarding flow, tutorial state, and user engagement lifecycle.
 * No knowledge of subscriptions or system settings.
 */
interface UserSessionManager {
    val isOnboardingComplete: Flow<Boolean>
    val isTutorialCompleted: StateFlow<Boolean>
    val launchCount: Flow<Int>

    suspend fun setOnboardingComplete(complete: Boolean)
    suspend fun setTutorialCompleted(completed: Boolean)
    suspend fun incrementLaunchCount()
    suspend fun getLaunchCount(): Int
    suspend fun saveLastReviewPromptTime(timestamp: Long)
    suspend fun getLastReviewPromptTime(): Long

    // Generic key-value store for engagement tracking
    suspend fun getInt(key: String, default: Int): Int
    suspend fun putInt(key: String, value: Int)
    suspend fun getString(key: String, default: String?): String?
    suspend fun putString(key: String, value: String)
    suspend fun getLong(key: String, default: Long): Long
    suspend fun putLong(key: String, value: Long)
    suspend fun getBoolean(key: String, default: Boolean): Boolean
    suspend fun putBoolean(key: String, value: Boolean)
}
```

---

### A3. CREATE file: `app/src/main/java/com/cryptodept/data/datastore/SubscriptionAccessManager.kt`

```kotlin
package com.cryptodept.data.datastore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Single source of truth for subscription tier, admin privileges, and AI usage limits.
 * No knowledge of UI settings or user session state.
 */
interface SubscriptionAccessManager {
    val isPro: StateFlow<Boolean>
    val isAdmin: StateFlow<Boolean>

    fun isAdmin(): Boolean
    fun getAdminStatusFlow(): Flow<Boolean>

    fun setProStatus(isPro: Boolean)
    fun setAdminStatus(isAdmin: Boolean)
    fun setProExpiry(durationDays: Int)
    fun checkProStatus()

    suspend fun getAiReportsCountToday(): Int
    suspend fun incrementAiReportsCount()
}
```

---

### A4. MODIFY file: `app/src/main/java/com/cryptodept/data/datastore/PreferencesService.kt`

Make `PreferencesService` implement all three interfaces. Do NOT change any internal logic, DataStore keys, or constructor parameters. Only add the `implements` clause:

```kotlin
// Change this line:
class PreferencesService @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext context: Context,
    private val securePrefs: SecurePrefsService,
)

// To this:
class PreferencesService @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext context: Context,
    private val securePrefs: SecurePrefsService,
) : SystemSettingsManager, UserSessionManager, SubscriptionAccessManager
```

All existing methods already satisfy the interface contracts. No body changes required.
Keep the existing `typealias PreferencesManager = PreferencesService` line as-is for backward compat.

---

### A5. CREATE file: `app/src/main/java/com/cryptodept/di/PreferencesModule.kt`

```kotlin
package com.cryptodept.di

import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.data.datastore.SubscriptionAccessManager
import com.cryptodept.data.datastore.SystemSettingsManager
import com.cryptodept.data.datastore.UserSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Exposes only the needed interface contract to each injection site.
 * PreferencesService itself remains a @Singleton and is constructed by Hilt
 * via its own @Inject constructor — no manual provide() needed for it.
 */
@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    fun provideSystemSettings(service: PreferencesService): SystemSettingsManager = service

    @Provides
    fun provideUserSession(service: PreferencesService): UserSessionManager = service

    @Provides
    fun provideSubscriptionAccess(service: PreferencesService): SubscriptionAccessManager = service
}
```

> **Note on scope:** The three interface bindings are NOT `@Singleton` intentionally — Hilt will always resolve them to the same underlying `@Singleton PreferencesService` instance. Adding `@Singleton` here would be redundant and could confuse the graph.

---

### A6. MIGRATE all injection sites

For each class below, replace `PreferencesService` with the narrowest interface that satisfies its actual usage. If a class needs properties from two different interfaces, inject both interfaces separately.

**Rule of thumb:**
- Uses only `soundsEnabled`, `soundsVolume`, `hapticEnabled`, `phosphorMode`, `refreshInterval`, `screensaverTimeout`, `powerUserMode`, `focusModeEnabled`, `crashlyticsConsent`, `notificationsEnabled` → inject **`SystemSettingsManager`**
- Uses only `isOnboardingComplete`, `isTutorialCompleted`, `launchCount`, review prompt times, generic `getInt/putInt/getLong/putLong/getString/putString/getBoolean/putBoolean` → inject **`UserSessionManager`**
- Uses only `isPro`, `isAdmin`, `setProStatus`, `setAdminStatus`, `setProExpiry`, `checkProStatus`, `getAiReportsCountToday`, `incrementAiReportsCount` → inject **`SubscriptionAccessManager`**

**Migration table (based on actual usages found in the codebase):**

| Class | File | Replace with |
|-------|------|-------------|
| `HapticService` | `util/HapticService.kt` | `SystemSettingsManager` |
| `TerminalAudioManager` | `util/TerminalAudioManager.kt` | `SystemSettingsManager` |
| `TerminalAudioService` | `util/TerminalAudioService.kt` | `SystemSettingsManager` |
| `TutorialController` | `domain-other/TutorialController.kt` | `UserSessionManager` |
| `ReviewService` | `util/ReviewService.kt` | `UserSessionManager` |
| `UserEngagementTracker` | `domain-usecase/UserEngagementTracker.kt` | `UserSessionManager` |
| `AuthService` | `data/AuthService.kt` | `SubscriptionAccessManager` |
| `BillingService` | `data/billing/BillingService.kt` | `SubscriptionAccessManager` |
| `TierAccessManager` | `domain-other/TierAccessManager.kt` | `SubscriptionAccessManager` |
| `GetDashboardDataUseCase` | `domain-usecase/GetDashboardDataUseCase.kt` | `SubscriptionAccessManager` |
| `GetDailyAIPickUseCase` | `domain-usecase/GetDailyAIPickUseCase.kt` | `UserSessionManager` (uses AI reports count) → `SubscriptionAccessManager` |
| `AchievementEngine` | `domain-other/AchievementEngine.kt` | Inspect actual usage, use narrowest fit |
| `DemoModeProvider` | `util/DemoModeProvider.kt` | Inspect actual usage, use narrowest fit |
| `AnalysisViewModel` | `viewmodel/AnalysisViewModel.kt` | `SubscriptionAccessManager` (uses `isAdmin`) |
| `AuthViewModel` | `viewmodel/AuthViewModel.kt` | `SubscriptionAccessManager` |
| `PortfolioViewModel` | `viewmodel/PortfolioViewModel.kt` | `SubscriptionAccessManager` (uses `isAdmin`) |
| `SignalsViewModel` | `viewmodel/SignalsViewModel.kt` | Inspect actual usage |
| `SettingsViewModel` | `viewmodel/SettingsViewModel.kt` | Inject BOTH `SystemSettingsManager` + `SubscriptionAccessManager` (it manages system settings AND reads `isAdmin` for the security warning) |
| `DashboardViewModel` | `viewmodel/DashboardViewModel.kt` | Inspect actual usage; likely `SubscriptionAccessManager` |
| `CryptoRepositoryImpl` | `data-repo/CryptoRepositoryImpl.kt` | Inspect actual usage |
| `FirebaseModule.provideCrashlytics` | `di/FirebaseModule.kt` | `SystemSettingsManager` (reads `crashlyticsConsent`) |
| `AppUpdateRepository` | `data-other/AppUpdateRepository.kt` | Inspect actual usage |

**Do NOT migrate** the following — they must retain the full `PreferencesService`:
- `AppModule.kt` (provides `AppUpdateRepository` and `TierAccessManager` where both need different slices)
- The `PreferencesModule.kt` you just created

**For tests** (`tests-unit/`): Replace `mockk<PreferencesService>(relaxed = true)` with `mockk<SubscriptionAccessManager>(relaxed = true)` or whichever interface the class under test now depends on. This makes tests dramatically simpler.

---

## TASK B — DECOUPLE `MultiSourcePriceAggregator` VIA STRATEGY PATTERN

### B1. MODIFY file: `app/src/main/java/com/cryptodept/util/SymbolResolver.kt`

Add four new exchange-specific mapping methods at the end of the class (before the closing `}`). These are the functions currently hardcoded inside `MultiSourcePriceAggregator` and must be migrated here:

```kotlin
// Add these four functions to SymbolResolver class:

fun toKrakenSymbol(coinGeckoId: String): String? =
    when (coinGeckoId) {
        "bitcoin" -> "XBTUSD"
        "ethereum" -> "ETHUSD"
        "ripple" -> "XRPUSD"
        "solana" -> "SOLUSD"
        "cardano" -> "ADAUSD"
        "polkadot" -> "DOTUSD"
        "dogecoin" -> "DOGEUSD"
        "chainlink" -> "LINKUSD"
        "shiba-inu" -> "SHIBUSD"
        "litecoin" -> "LTCUSD"
        "avalanche-2" -> "AVAXUSD"
        "tron" -> "TRXUSD"
        "matic-network" -> "MATICUSD"
        "stellar" -> "XLMUSD"
        "cosmos" -> "ATOMUSD"
        else -> "${coinGeckoId.uppercase()}USD"
    }

fun toCoinbaseSymbol(coinGeckoId: String): String? =
    when (coinGeckoId) {
        "bitcoin" -> "BTC-USD"
        "ethereum" -> "ETH-USD"
        "ripple" -> "XRP-USD"
        "solana" -> "SOL-USD"
        "cardano" -> "ADA-USD"
        "polkadot" -> "DOT-USD"
        "dogecoin" -> "DOGE-USD"
        "chainlink" -> "LINK-USD"
        "shiba-inu" -> "SHIB-USD"
        "litecoin" -> "LTC-USD"
        "avalanche-2" -> "AVAX-USD"
        "tron" -> "TRX-USD"
        "matic-network" -> "MATIC-USD"
        "stellar" -> "XLM-USD"
        "cosmos" -> "ATOM-USD"
        else -> "${coinGeckoId.uppercase()}-USD"
    }

fun toCoinCapId(coinGeckoId: String): String =
    when (coinGeckoId) {
        "avalanche-2" -> "avalanche"
        "matic-network" -> "polygon"
        else -> coinGeckoId.lowercase()
    }

fun toCoinPaprikaId(coinGeckoId: String): String? =
    when (coinGeckoId) {
        "bitcoin" -> "btc-bitcoin"
        "ethereum" -> "eth-ethereum"
        "ripple" -> "xrp-ripple"
        "solana" -> "sol-solana"
        "cardano" -> "ada-cardano"
        "polkadot" -> "dot-polkadot"
        "dogecoin" -> "doge-dogecoin"
        "chainlink" -> "link-chainlink"
        "shiba-inu" -> "shib-shiba-inu"
        "litecoin" -> "ltc-litecoin"
        "avalanche-2" -> "avax-avalanche"
        "tron" -> "trx-tron"
        "matic-network" -> "matic-polygon"
        "stellar" -> "xlm-stellar"
        "cosmos" -> "atom-cosmos"
        else -> null // Unknown coin — return null to signal skip
    }
```

---

### B2. CREATE file: `app/src/main/java/com/cryptodept/domain/repository/PriceProvider.kt`

```kotlin
package com.cryptodept.domain.repository

/**
 * Strategy interface for a single price data source.
 * Each exchange implementation knows only about itself.
 * MultiSourcePriceAggregator does not know which exchanges exist.
 */
interface PriceProvider {
    /** Human-readable name used for logging and cache keys. */
    val providerName: String

    /**
     * Fetch the current price for the given CoinGecko ID.
     * @return Result.success(price) or Result.failure(exception) — never throws.
     */
    suspend fun fetchPrice(coinGeckoId: String): Result<Double>
}
```

---

### B3. CREATE file: `app/src/main/java/com/cryptodept/data/api/KrakenPriceProvider.kt`

```kotlin
package com.cryptodept.data.api

import com.cryptodept.domain.repository.PriceProvider
import com.cryptodept.util.SymbolResolver
import javax.inject.Inject

class KrakenPriceProvider @Inject constructor(
    private val krakenApi: KrakenApi,
    private val symbolResolver: SymbolResolver,
) : PriceProvider {

    override val providerName = "KRAKEN"

    override suspend fun fetchPrice(coinGeckoId: String): Result<Double> {
        val symbol = symbolResolver.toKrakenSymbol(coinGeckoId)
            ?: return Result.failure(IllegalArgumentException("No Kraken mapping for: $coinGeckoId"))
        return runCatching {
            krakenApi.getTicker(symbol).result.values.first().lastPrice
        }
    }
}
```

---

### B4. CREATE file: `app/src/main/java/com/cryptodept/data/api/CoinbasePriceProvider.kt`

```kotlin
package com.cryptodept.data.api

import com.cryptodept.domain.repository.PriceProvider
import com.cryptodept.util.SymbolResolver
import javax.inject.Inject

class CoinbasePriceProvider @Inject constructor(
    private val coinbaseApi: CoinbaseApi,
    private val symbolResolver: SymbolResolver,
) : PriceProvider {

    override val providerName = "COINBASE"

    override suspend fun fetchPrice(coinGeckoId: String): Result<Double> {
        val symbol = symbolResolver.toCoinbaseSymbol(coinGeckoId)
            ?: return Result.failure(IllegalArgumentException("No Coinbase mapping for: $coinGeckoId"))
        return runCatching {
            coinbaseApi.getProductTicker(symbol).lastPrice
        }
    }
}
```

---

### B5. CREATE file: `app/src/main/java/com/cryptodept/data/api/CoinCapPriceProvider.kt`

```kotlin
package com.cryptodept.data.api

import com.cryptodept.domain.repository.PriceProvider
import com.cryptodept.util.SymbolResolver
import javax.inject.Inject

class CoinCapPriceProvider @Inject constructor(
    private val coinCapApi: CoinCapApi,
    private val symbolResolver: SymbolResolver,
) : PriceProvider {

    override val providerName = "COINCAP"

    override suspend fun fetchPrice(coinGeckoId: String): Result<Double> {
        val assetId = symbolResolver.toCoinCapId(coinGeckoId)
        return runCatching {
            coinCapApi.getAsset(assetId).data.lastPrice
        }
    }
}
```

---

### B6. CREATE file: `app/src/main/java/com/cryptodept/data/api/CoinPaprikaPriceProvider.kt`

```kotlin
package com.cryptodept.data.api

import com.cryptodept.domain.repository.PriceProvider
import com.cryptodept.util.SymbolResolver
import javax.inject.Inject

class CoinPaprikaPriceProvider @Inject constructor(
    private val coinPaprikaApi: CoinPaprikaApi,
    private val symbolResolver: SymbolResolver,
) : PriceProvider {

    override val providerName = "COINPAPRIKA"

    override suspend fun fetchPrice(coinGeckoId: String): Result<Double> {
        val paprikaId = symbolResolver.toCoinPaprikaId(coinGeckoId)
            ?: return Result.failure(IllegalArgumentException("No Paprika mapping for: $coinGeckoId"))
        return runCatching {
            coinPaprikaApi.getTicker(paprikaId).lastPrice
        }
    }
}
```

---

### B7. CREATE file: `app/src/main/java/com/cryptodept/di/PriceProvidersModule.kt`

```kotlin
package com.cryptodept.di

import com.cryptodept.data.api.CoinCapPriceProvider
import com.cryptodept.data.api.CoinPaprikaPriceProvider
import com.cryptodept.data.api.CoinbasePriceProvider
import com.cryptodept.data.api.KrakenPriceProvider
import com.cryptodept.domain.repository.PriceProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Registers all PriceProvider implementations into a Set<PriceProvider>
 * via Hilt Multibindings. To add a new exchange (e.g. Binance), simply
 * create a new XxxPriceProvider class and add a @Binds @IntoSet here.
 * MultiSourcePriceAggregator does NOT need to change.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PriceProvidersModule {

    @Binds
    @IntoSet
    abstract fun bindKrakenProvider(impl: KrakenPriceProvider): PriceProvider

    @Binds
    @IntoSet
    abstract fun bindCoinbaseProvider(impl: CoinbasePriceProvider): PriceProvider

    @Binds
    @IntoSet
    abstract fun bindCoinCapProvider(impl: CoinCapPriceProvider): PriceProvider

    @Binds
    @IntoSet
    abstract fun bindCoinPaprikaProvider(impl: CoinPaprikaPriceProvider): PriceProvider
}
```

---

### B8. MODIFY file: `app/src/main/java/com/cryptodept/data/api/MultiSourcePriceAggregator.kt`

Replace the entire file content with the decoupled version below. This version:
- Removes all 4 direct API dependencies
- Removes all 4 `mapToXxx()` private functions (they now live in `SymbolResolver`)
- Retains the existing `AggregatedPrice` return type, `lastPrices` cache, `updatePriceFromWS()`, and `calculateFromCache()` / `calculateMedian()` logic — **DO NOT change the calculation logic**
- The `binancePrice` parameter in `fetchAggregatedPrice` is retained for WebSocket compatibility

```kotlin
package com.cryptodept.data.api

import com.cryptodept.domain.model.AggregatedPrice
import com.cryptodept.domain.repository.PriceProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiSourcePriceAggregator @Inject constructor(
    private val providers: Set<@JvmSuppressWildcards PriceProvider>,
) {
    private val lastPrices = mutableMapOf<String, MutableMap<String, Double>>()

    suspend fun fetchAggregatedPrice(
        coinId: String,
        binancePrice: Double?,
    ): AggregatedPrice = coroutineScope {
        val deferredPrices = providers.map { provider ->
            async {
                provider.fetchPrice(coinId).getOrNull()?.let { price ->
                    provider.providerName.lowercase() to price
                }
            }
        }

        val coinCache = lastPrices.getOrPut(coinId) { mutableMapOf() }
        deferredPrices.awaitAll().filterNotNull().forEach { (sourceName, price) ->
            coinCache[sourceName] = price
        }
        binancePrice?.let { coinCache["binance"] = it }

        calculateFromCache(coinId, binancePrice)
    }

    fun updatePriceFromWS(
        coinId: String,
        source: String,
        price: Double,
    ): AggregatedPrice {
        val coinCache = lastPrices.getOrPut(coinId) { mutableMapOf() }
        coinCache[source] = price
        return calculateFromCache(coinId, coinCache["binance"])
    }

    private fun calculateFromCache(
        coinId: String,
        currentBinance: Double?,
    ): AggregatedPrice {
        val coinCache = lastPrices[coinId] ?: mutableMapOf()
        val validPrices = coinCache.values.filter { it > 0 }
        val median = calculateMedian(validPrices)

        val maxPrice = validPrices.maxOrNull() ?: median
        val minPrice = validPrices.minOrNull() ?: median
        val deviation = if (median > 0) ((maxPrice - minPrice) / median) * 100 else 0.0

        return AggregatedPrice(
            coinId = coinId,
            binancePrice = currentBinance,
            krakenPrice = coinCache["kraken"],
            coinbasePrice = coinCache["coinbase"],
            coincapPrice = coinCache["coincap"],
            coinpaprikaPrice = coinCache["paprika"],
            consensusPrice = median,
            maxDeviationPercent = deviation,
            isReliable = deviation < 0.5 && validPrices.size >= 3,
            sourcesCount = validPrices.size,
        )
    }

    private fun calculateMedian(list: List<Double>): Double {
        if (list.isEmpty()) return 0.0
        val sorted = list.sorted()
        return if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2] + sorted[sorted.size / 2 - 1]) / 2
        } else {
            sorted[sorted.size / 2]
        }
    }
}
```

> **IMPORTANT:** In `calculateFromCache`, the `AggregatedPrice` named parameters `krakenPrice`, `coinbasePrice`, `coincapPrice`, `coinpaprikaPrice` now use cache keys that match `provider.providerName.lowercase()`. Verify that `KrakenPriceProvider.providerName = "KRAKEN"` → cache key = `"kraken"`, which correctly maps to the `krakenPrice` named argument. If the providerName casing causes a mismatch, align them: either change `providerName` in providers or change the cache key lookup in `calculateFromCache`. The simplest fix: use a companion constant in each provider class, e.g. `companion object { const val NAME = "kraken" }` and use that as both `providerName` and as the map key.

---

## TASK C — VERIFY NO REGRESSIONS

After completing all changes, perform the following checks:

1. **Build:** Run `./gradlew assembleDebug`. There must be zero compilation errors.
2. **Hilt graph:** Run `./gradlew hiltAggregateDeps`. Verify no "missing binding" errors.
3. **Unit tests:** Run `./gradlew testDebugUnitTest`. All existing tests must pass. The mocked tests in `tests-unit/` that used `mockk<PreferencesService>(relaxed = true)` should now mock the narrower interface instead.
4. **No DataStore key changes:** Confirm that the `companion object` keys in `PreferencesService` (`REFRESH_INTERVAL`, `PHOSPHOR_MODE`, etc.) are completely unchanged. No data migration is needed.
5. **Backward compat alias:** Confirm `typealias PreferencesManager = PreferencesService` and `typealias BillingManager = BillingService` still exist.

---

## SUMMARY OF NEW FILES

| File | Purpose |
|------|---------|
| `data/datastore/SystemSettingsManager.kt` | Interface: visual/audio config |
| `data/datastore/UserSessionManager.kt` | Interface: onboarding, tutorials, engagement |
| `data/datastore/SubscriptionAccessManager.kt` | Interface: Pro/Admin status, AI limits |
| `di/PreferencesModule.kt` | Hilt: exposes 3 interfaces from 1 singleton |
| `domain/repository/PriceProvider.kt` | Strategy interface for price sources |
| `data/api/KrakenPriceProvider.kt` | Kraken exchange implementation |
| `data/api/CoinbasePriceProvider.kt` | Coinbase exchange implementation |
| `data/api/CoinCapPriceProvider.kt` | CoinCap exchange implementation |
| `data/api/CoinPaprikaPriceProvider.kt` | CoinPaprika exchange implementation |
| `di/PriceProvidersModule.kt` | Hilt Multibindings: Set<PriceProvider> |

## SUMMARY OF MODIFIED FILES

| File | Change |
|------|--------|
| `data/datastore/PreferencesService.kt` | Add `: SystemSettingsManager, UserSessionManager, SubscriptionAccessManager` |
| `util/SymbolResolver.kt` | Add 4 exchange-specific mapping methods |
| `data/api/MultiSourcePriceAggregator.kt` | Replace with provider-agnostic version |
| 15+ ViewModel/Service/UseCase files | Replace `PreferencesService` injection with narrowest interface |
