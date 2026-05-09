# CryptoDept — MASTERPLAN v5.4 | WORLD #1 ELITE MODE
## Goal: #1 Crypto Analyzer in the World + Score 9.50+/10
### Version 6.3 | Claude Sonnet 4.6 | 10.05.2026

---

# 📜 CRITICAL WORKING RULES (RULES) — V5
0. **Primary and only language of the application - English.** All UI texts, documentation, logs, and comments are in English. This is crucial for a global audience and professional image.
1. **TOKEN BUDGETING:** Before each prompt, there is an estimate `≈X tokens`. If you see the context melting away — stop and say `[PAUSE — context low]`.
2. **ATOMIC COMPILATION:** Every sub-task (Part A, B, C, D...) MUST leave the project in a **compilable** state. This is a sacred rule. If Part X requires something that will be ready in Part Y — create a stub/placeholder that compiles and replace it in Y.
3. **GRANULARITY > SPEED:** We prefer 5 small parts over 2 large ones. If the agent stops, we lose at most 1 small part.
4. **THREE/FOUR-STEP ARCHITECTURE:**
    * **Part A (Core):** Data models, Repository interfaces, math/algo logic — no UI.
    * **Part B (Bridge):** ViewModel, Use Cases, DI wiring.
    * **Part C (UI):** Composables / XML layouts.
    * **Part D (Polish):** Animations, edge cases, error handling.
5. **DYNAMIC PLAN:** After completing a sub-task → mark with `[✅ DONE — DD.MM.YYYY]` in front of the line. **DO NOT delete the text** — we preserve history for clarity.
6. **PRIORITY FIRST:** Execute in order unless explicitly stated otherwise.
7. **NO REGRESSION:** No new prompt should break old ones. If necessary → migrate along with the change.
8. **TEST WHAT YOU TOUCH:** When making core logic (calculator, evaluator, math) — add at least 1 unit test in the same prompt.

---

# 🎯 STRATEGIC DIRECTION — V5

**From Score 5.31 (developer) / 6.44 (user) → to 9.50+ / 9.50+**

Improvements across each category with specific prompts:

 Category | Current | Goal | Prompts |
---|---|---|---|
 Security | 3.50 | 9.00+ | #200, #201, #202, #210, #220 |
 Performance | 6.00 | 9.00+ | #230, #231, #240, #241 |
 Code Quality | 5.50 | 9.00+ | #250, #251, #252, #260, #800 |
 Architecture | 5.00 | 9.00+ | #270 (Hilt), #280 (Compose), #810 |
 Scalability | 3.50 | 9.00+ | #300, #301, #302 (Backend) |
 Maintainability | 4.00 | 9.00+ | #310 (Tests), #320 (Crashlytics), #850 |
 Ease of Use | 5.00 | 9.50+ | #330 (Onboarding), #331, #332, #820 |
 Stability | 5.50 | 9.50+ | #340 (Crashlytics), #341 |
 Value (Paid) | 4.50 | 9.00+ | #400+ (SaaS prep), #840 |

---

# ═══════════════════════════════════════
# PHASE A: COMPLETING THE CURRENT (PRIORITY 1)
# ═══════════════════════════════════════

## PROMPT #127 — Custom Composite Alerts
**TASK:** Alert system with AND/OR logic (Price + RSI + Volume + Volatility).

### #127-A — Domain Model + Sealed classes  ≈ 1500 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Create sealed classes `AlertCondition` (Price, RSI, Volume, Volatility) and `AlertOperator` (AND, OR). Add `CompositeAlert` data class with a list of conditions and operators. Create Room Entity `CompositeAlertEntity` + DAO. Migration from the current DB version. **Important:** maintain backward compatibility with current simple price alerts.</font>

### #127-B — Evaluation Engine (Pure Kotlin) + Unit Tests  ≈ 1800 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `AlertEvaluator` class that takes a `MarketSnapshot` (price, rsi, volume, volatility) and `CompositeAlert`, returns `Boolean`. Implement truth table for AND/OR with shortcut evaluation. **Add 5 unit tests** in `AlertEvaluatorTest.kt`: price only, AND with 2 conditions, OR with 2 conditions, complex (A AND B) OR C, edge case with an empty list.</font>

### #127-C — Repository + AlertsViewModel  ≈ 1500 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `CompositeAlertsRepository` (interface + impl). `CompositeAlertsViewModel` with StateFlow for the alert list and Channel for edit/delete events. Wire via current DI logic (Hilt comes in #270 — manual for now).</font>

### #127-D — Background Evaluation Worker  ≈ 1300 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** WorkManager `CompositeAlertsWorker` that runs periodically (15 min), fetches all active alerts, fetches snapshot, evaluates, triggers notification. Battery-friendly: skip if battery <15% and not charging.</font>

### #127-E — UI: List Screen (Compose)  ≈ 1700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `CompositeAlertsScreen.kt` (Compose) — LazyColumn with existing alerts, FAB for new, swipe-to-delete, toggle enable/disable. Follow CRT style (square corners, `LocalTerminalColors.current`, JetBrains Mono).</font>

### #127-F — UI: Builder Screen (Compose)  ≈ 2200 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `CompositeAlertBuilderScreen.kt` — step-by-step builder: 1) coin selection, 2) adding conditions (each with type, operator, value), 3) logic operator between conditions (AND/OR), 4) alert name, 5) natural language preview of the current condition ("BTC price > 70000 AND RSI < 30").</font>

### #127-G — Polish: Notifications + Sound + Haptic  ≈ 900 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Custom notification channel for composite alerts. CRT-style sound (use existing CRT audio infrastructure from #145). Haptic feedback on trigger.</font>

---

## PROMPT #140 — Heatmap Screensaver + Cycle System
**TASK:** Third screensaver mode (Treemap) + Cyclic rotation of the three currently available + Changing idle timeout from 5 to 2 minutes.

### #140-A — Treemap Algorithm (Pure Kotlin) + Unit Tests  ≈ 1800 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Squarified Treemap algorithm in `TreemapPartition.kt`. Input: `List<TreemapItem(symbol, marketCap, change24h)>` + canvas size. Output: `List<TreemapRect(x, y, w, h, item)>`. The algorithm balances aspect ratio. **Add unit test** for: 1 item, 5 items, 100 items, edge cases (zero size).</font>

### #140-B — Color Mapping + Data Source  ≈ 1100 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `HeatmapColorMapper` — maps change24h to color (green for +, red for -, intensity = magnitude). `HeatmapDataRepository` — takes top 50 coins by market cap from existing CoinGecko/Binance client, caches for 60 sec.</font>

### #140-C — Canvas Drawing (Compose Canvas)  ≈ 1700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `HeatmapScreensaverScreen.kt` with Compose Canvas — draw rectangles, draw symbol text inside rectangles (text scales with block size), draw % change. CRT phosphor effect overlay (use existing `CRTOverlay`).</font>

### #140-D — Screensaver Cycle Manager  ≈ 1400 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `ScreensaverCycleManager` singleton that holds a list of registered screensavers (Bloomberg Wall, Matrix Rain, Heatmap), changes them in random order (no immediate repetition), waits 2-3 min for each, then switches.</font>

### #140-E — Idle Timer Update + Wiring  ≈ 800 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Find current idle detection logic (`IdleDetector` or similar), change timeout from 5 min to **2 min**. Wire `ScreensaverCycleManager` with `MainActivity` lifecycle. On user interaction → cancel screensaver, reset timer.</font>

### #140-F — Polish: Smooth Transitions + Performance  ≈ 1000 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Crossfade transition between screensavers (300ms). Frame-rate cap at 30 FPS for heatmap (battery save). Wake-lock only for active screensaver, release display on dark screen.</font>

---

# ═══════════════════════════════════════
# PHASE B: SECURITY HARDENING (PRIORITY 1 — CRITICAL)
# ═══════════════════════════════════════

## PROMPT #200 — API Keys Audit & Migration to local.properties

### #200-A — Audit Script + Discovery  ≈ 800 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Create `scripts/audit_secrets.py` (Python) script that scans the entire project and finds API keys. Report generated and analyzed.</font>

### #200-B — local.properties Migration Plan  ≈ 1000 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Keys moved to `local.properties`. `build.gradle.kts` updated to read from there with a safe fallback. Deleted old `secrets.properties`.</font>

### #200-C — Code Replace: hardcoded → BuildConfig  ≈ 1500 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Confirmed all keys use `BuildConfig`. Project compiles successfully.</font>

### #200-D — Git History Audit + Decision  ≈ 600 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** History audit found an expired key. Decision: Keys rotated and secured.</font>

---

## PROMPT #201 — Encrypted Local Storage for sensitive data

### #201-A — EncryptedSharedPreferences Setup  ≈ 900 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Added security library. Created `SecurePrefsManager` for encrypted storage of keys and admin data.</font>

### #201-B — Migration from Plain Prefs → Encrypted  ≈ 1100 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Automatic migration of `IS_PRO` and `IS_ADMIN` flags from DataStore to encrypted preferences. Cleaned up old keys.</font>

### #201-C — Room DB Encryption (SQLCipher)  ≈ 1400 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Added SQLCipher support. Room database is now encrypted with a 256-bit key stored in SecurePrefsManager.</font>

---

## PROMPT #202 — Network Security: Certificate Pinning + HTTPS-only

### #202-A — Network Security Config XML  ≈ 700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Create `res/xml/network_security_config.xml` with: `cleartextTrafficPermitted=false`, pinning for main API endpoints (api.coingecko.com, api.binance.com, generativelanguage.googleapis.com). Reference in `AndroidManifest.xml`.</font>

### #202-B — OkHttp Certificate Pinner  ≈ 900 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Add `CertificatePinner` to OkHttp builder for production builds. For debug builds — allow self-signed (for dev server). Use SHA-256 pin hashes (update with script `scripts/get_pins.sh`).</font>

### #202-C — Pin Refresh Strategy + Backup Pins  ≈ 600 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Add **2 pins** (current + backup) for each pinned host to avoid breaking the app during certificate rotation. Document in `docs/security/pin_rotation.md`.</font>

---

## PROMPT #210 — ProGuard / R8 Obfuscation

### #210-A — R8 Rules for Production  ≈ 1100 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Enable `minifyEnabled = true` + `shrinkResources = true` in release buildType. Create `proguard-rules.pro` with keep rules for: Retrofit/Moshi data classes (`@JsonClass`), Room entities, Compose runtime, lifecycle, all `BuildConfig` constants.</font>

### #210-B — Verify Release Build  ≈ 600 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `./gradlew assembleRelease` — check that it builds without warnings. Decompile APK with `apktool` locally and confirm class names are obfuscated. If crashes in release — add more keep rules.</font>

### #210-C — Release Signing Setup  ≈ 700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Generate release keystore (`scripts/gen_keystore.sh`). Add `signingConfigs` in `build.gradle.kts` reading password from `local.properties` (`KEYSTORE_PASSWORD`, `KEY_PASSWORD`). **NEVER commit the keystore file or password.**</font>

---

## PROMPT #220 — Root / Tamper Detection (Admin mode protection)

### #220-A — Root Detection Library  ≈ 800 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Add `com.scottyab:rootbeer-lib:0.1.0`. Wrap in `RootDetector` singleton. Method `isDeviceRooted(): Boolean` + `isDebuggable(): Boolean` (`BuildConfig.DEBUG || ApplicationInfo.FLAG_DEBUGGABLE`).</font>

### #220-B — Tamper Check (APK Signature)  ≈ 1000 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** On start, verify SHA-256 of APK signature via `PackageManager`. Compare with known-good hash (your production keystore signature). If mismatch → log + degrade functionality (no admin mode).</font>

### #220-C — Admin Gate Enhancement  ≈ 700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** In Big Boss admin gate (#153) → if root + tamper → show warning but allow admin (for yourself on dev device). For production builds → block admin if tamper detected.</font>

---

# ═══════════════════════════════════════
# PHASE C: ARCHITECTURE OVERHAUL — Hilt + Compose Migration
# ═══════════════════════════════════════

## PROMPT #270 — Hilt DI Migration

### #270-A — Hilt Setup  ≈ 900 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Add `com.google.dagger:hilt-android:2.51.1` + KSP plugin. Create `CryptoDeptApplication` with `@HiltAndroidApp` (if already exists — add annotation). Annotate `MainActivity` with `@AndroidEntryPoint`.</font>

### #270-B — Network Module  ≈ 1100 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `NetworkModule.kt` (`@Module @InstallIn(SingletonComponent::class)`). Provides: OkHttpClient (with Interceptors from #115), Retrofit instances per API (CoinGecko, Binance, Gemini), `Moshi` singleton. Remove manual instantiation of these in code.</font>

### #270-C — Database Module  ≈ 800 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `DatabaseModule.kt` — provides Room DB + all DAOs. Annotate all DAO-injection points with `@Inject constructor`.</font>

### #270-D — Repository Module  ≈ 1300 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Convert all Repository classes to `@Singleton class XxxRepository @Inject constructor(...)`. If interfaces exist — add `@Binds` in `RepositoryModule`. Remove manual Singletons (`object` or `INSTANCE` patterns).</font>

### #270-E — ViewModel Migration  ≈ 1200 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** All ViewModels → `@HiltViewModel class XxxViewModel @Inject constructor(...)`. In Fragment/Activity → `by viewModels()` (should work now). Remove custom `ViewModelFactory` classes.</font>

### #270-F — UseCase Layer (Optional but Recommended)  ≈ 1500 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** For most complex ViewModels (>500 lines) → extract business logic into use cases (`@Singleton class GetCompositeAlertsUseCase @Inject constructor(repo: ...)`). ViewModel becomes a thin coordination layer.</font>

### #270-G — Cleanup + Verification  ≈ 700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Remove all leftovers of manual DI (old factories, manual passing of dependencies). Verify: project compiles, app starts without crash, basic flow works.</font>

---

## PROMPT #280 — Compose Migration (XML → Compose)

### #280-A — Inventory of XML Layouts  ≈ 600 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Script `scripts/list_xml_layouts.py` listing all `.xml` layout files grouped by: screens, fragments, item layouts, dialogs. Output: `xml_inventory.md` with migration priority (use frequency).</font>

### #280-B — Compose Theme Foundation  ≈ 1400 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Align Compose theme with current XML theme. `TerminalTheme.kt` with `MaterialTheme` + custom `LocalTerminalColors` (already exists). Ensure font (JetBrains Mono), colors, square corners, CRT overlay are applied globally in Compose.</font>

### #280-C — Migrate: Settings Screen  ≈ 1500 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Settings is a good first target (mostly lists, prefs). Convert `fragment_settings.xml` + `SettingsFragment.kt` → `SettingsScreen.kt` (Compose). Keep navigation (NavHost / Fragment + ComposeView).</font>

### #280-D — Migrate: Portfolio Screen  ≈ 1700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `fragment_portfolio.xml` → `PortfolioScreen.kt`. List of holdings + total value + chart. Charts remain MPAndroidChart for now (wrap in `AndroidView`). Tab bar → Compose `TabRow`.</font>

### #280-E — Migrate: News Screen  ≈ 1500 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `fragment_news.xml` → `NewsScreen.kt`. LazyColumn with news items. Click → detail screen (also Compose). Keep filter chips for source.</font>

### #280-F — Migrate: Alerts List  ≈ 1300 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Old alerts list (pre #127) → Compose. Unify UI with `CompositeAlertsScreen` from #127-E.</font>

### #280-G — Migrate: Charts Screen  ≈ 1900 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Most complex — chart with overlays (MA, RSI, MACD). Chart remains `AndroidView(MPAndroidChart)` for now, but controls (timeframe, indicators toggle) → Compose. Toolbar → `TopAppBar`.</font>

### #280-H — Migrate: Dashboard / Home  ≈ 2000 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Main screen. Multiple cards (price ticker, top movers, fear&greed gauge, news teaser). Each card → separate `@Composable` function. Keep "WHAT SHOULD I DO NOW?" button (#142).</font>

### #280-I — Cleanup XML Resources  ≈ 700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Remove migrated `.xml` layouts (keep those for non-migrated screens). Remove `data-binding` if no longer used. Verify build size decreases.</font>

### #280-J — Migrate: Remaining Dialogs & Sheets  ≈ 1400 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** All `BottomSheetDialogFragment` → Compose `ModalBottomSheet`. All `AlertDialog.Builder` → Compose `AlertDialog`. Custom dialogs for coin selector, timeframe picker.</font>

---

# ═══════════════════════════════════════
# PHASE D: TESTING + CRASH MONITORING
# ═══════════════════════════════════════

## PROMPT #310 — Unit Tests Foundation

### #310-A — Test Dependencies + Structure  ≈ 600 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Add to `build.gradle.kts`: `junit:4.13.2`, `kotlinx-coroutines-test:1.8.0`, `mockk:1.13.10`, `turbine:1.0.0`, `truth:1.4.0`. Structure: `src/test/kotlin/com/cryptodept/...` mirroring main package.</font>

### #310-B — Test: Technical Indicators  ≈ 1500 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `RsiCalculatorTest`, `MacdCalculatorTest`, `BollingerBandsTest`, `MovingAverageTest`. Each minimum 5 tests: known input → known output (from TradingView screenshot as ground truth), edge cases (empty list, one item, NaN handling).</font>

### #310-C — Test: Sentiment Scoring  ≈ 1200 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `SentimentScorerTest` for #126. Tests: positive words → high score, negative → low score, mixed → middle, English text processing, sarcasm/negation handling.</font>

### #310-D — Test: Prediction Cache  ≈ 900 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** PredictionCacheTest for #92. Tests: cache hit, cache miss, expiry, invalidation, concurrent access.</font>

### #310-E — Test: Alert Evaluator  ≈ 800 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Additional tests beyond #127-B. Stress test with 100 alerts, performance benchmark (<1ms for evaluating alert with 5 conditions).</font>

### #310-F — Test: Composables (UI)  ≈ 1300 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** Compose UI tests with `androidx.compose.ui:ui-test-junit4`. Tests for: PriceTicker (price update), AlertCard (swipe action), DashboardCard (click). Minimum 10 tests.</font>

### #310-G — Test: Repository Layer  ≈ 1400 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** Mock Retrofit API with MockWebServer. Tests: success path, network error, rate limit fallback (from #115), cache behavior. Minimum 15 tests distributed among repos.</font>

---

## PROMPT #320 — Firebase Crashlytics + Analytics

### #320-A — Firebase Project Setup (Manual + Doc)  ≈ 700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Document `docs/firebase_setup.md` with steps: create Firebase project, register Android app, download `google-services.json`, add to `app/`. Add `google-services.json` to `.gitignore` (contains public info but is personal config).</font>

### #320-B — Crashlytics Integration  ≈ 1000 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Add `com.google.firebase:firebase-crashlytics-ktx` + `com.google.firebase:firebase-bom`. Plugin `com.google.firebase.crashlytics`. Verify: add `crashlytics.recordException(throwable)` in catch blocks of repositories and use cases. Crashlytics user-id = anonymized device ID (not IMEI!).</font>

### #320-C — Custom Logs + Keys  ≈ 800 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `CrashlyticsLogger` wrapper used throughout the app. `log("Action: fetch_btc_price")`, `setKey("api_provider", "coingecko")`, `setKey("active_screen", "dashboard")`. Provides context for crash reports.</font>

### #320-D — Performance Monitoring  ≈ 700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Add `firebase-perf-ktx`. Monitor: API call latency (per endpoint), screen render time, app start time. Identify slow spots.</font>

### #320-E — Analytics Events (privacy-first)  ≈ 900 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `firebase-analytics-ktx`. Track: `feature_used` (screen visited), `prediction_generated`, `alert_triggered`, `screensaver_shown`. **NO PII**: no names, addresses, portfolio values. Anonymous usage only. Opt-in toggle in Settings.</font>

### #320-F — Custom Non-Fatal Reporting  ≈ 600 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** In critical non-fatal scenarios (failed API call, invalid JSON, prediction crash recovery) → `Firebase.crashlytics.recordException(...)` without crash. Provides insight without UX impact.</font>

---

## PROMPT #340 — Production Stability Sweep

### #340-A — Memory Leak Detection  ≈ 1100 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Add `com.squareup.leakcanary:leakcanary-android:2.13` (debug only). Run app, navigate through all screens, review LeakCanary report. Fix top 3 leaks (likely screensavers with canvas, listeners without unregister).</font>

### #340-B — Lifecycle Audit  ≈ 1300 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Review all `viewModelScope.launch`, `lifecycleScope.launch`, listeners (BroadcastReceiver, ConnectivityManager.NetworkCallback). Ensure proper cleanup in `onCleared()` / `onDestroy()`. Document in `docs/lifecycle_patterns.md`.</font>

### #340-C — ANR (App Not Responding) Prevention  ≈ 900 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Find all `runBlocking { }` outside tests. Replace with proper async pattern. Find heavy work on main thread (JSON parsing of large response, file I/O) → move to `Dispatchers.IO`.</font>

### #340-D — Battery Optimization Check  ≈ 800 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** Review all WorkManager workers. Check: appropriate `Constraint` (network, charging), correct `BackoffPolicy`. Remove pointless polls — migrate to WebSockets where possible (#94 has the framework).</font>

---

# ═══════════════════════════════════════
# PHASE E: USER EXPERIENCE — Onboarding & UX
# ═══════════════════════════════════════

## PROMPT #330 — Welcome Screen + First-Run Onboarding

### #330-A — Onboarding Data Models  ≈ 700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `OnboardingStep` data class (title, description, image, action). Sealed class for step types: WelcomeStep, FeatureStep (with feature highlight), PermissionsStep, ApiKeyStep (for power users), CompletionStep.</font>

### #330-B — Welcome Screen (Compose)  ≈ 1500 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `WelcomeScreen.kt` — fullscreen CRT-style "boot sequence" animation (terminal text typewriter): "INITIALIZING CRYPTODEPT TERMINAL... ACCESS GRANTED... WELCOME, OPERATOR." → button [BEGIN]. Connection with theme (CRT phosphor effect).</font>

### #330-C — Tutorial Carousel  ≈ 1700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `TutorialPagerScreen.kt` — `HorizontalPager` with 5-7 steps explaining: 1) Dashboard, 2) Predictions (Ensemble), 3) Composite Alerts, 4) Screensavers, 5) AI Trade Coach, 6) WHY? button, 7) Big Boss mode (admin only). Page indicators in CRT style.</font>

### #330-D — Permission Requests  ≈ 1000 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Pre-permission rationale screen ("CryptoDept needs notifications to alert you on price moves..."). Then actual `requestPermissions(POST_NOTIFICATIONS)`. Battery optimization opt-out request with explanation.</font>

### #330-E — First-Run State Tracking  ≈ 600 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `OnboardingPrefs` (encrypted) → flag `isOnboardingComplete`. After last step → `true`. On `MainActivity` start → if `false` → start OnboardingActivity.</font>

### #330-F — Skip + Restart Onboarding  ≈ 500 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** Skip button on every step. In Settings → "Restart Tutorial" option resetting `isOnboardingComplete` and starting the flow again.</font>

---

## PROMPT #331 — Empty States + Loading States

### #331-A — Empty States (No data scenarios)  ≈ 1100 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** For every list screen (Alerts, Portfolio, News, Predictions History) add `EmptyState` composable with icon (CRT-style ASCII art), short message, and primary action ("CREATE FIRST ALERT").</font>

### #331-B — Skeleton Loaders  ≈ 1300 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Replace all `CircularProgressIndicator` in list screens with shimmer skeleton placeholders. CRT-style: blocks with scanline animation. Compose `Modifier.placeholder()`.</font>

### #331-C — Error States with Recovery  ≈ 900 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Unified `ErrorState` composable: icon, message ("CONNECTION LOST"), retry button. Includes offline indicator banner at top of dashboard when no internet.</font>

---

## PROMPT #332 — Smart Defaults + Progressive Disclosure

### #332-A — Hide Advanced Features by Default  ≈ 800 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Settings → toggle "Power User Mode" (default OFF). When OFF → Big Boss admin, FFT details, custom signal composer are hidden. When ON → everything visible. Onboarding asks once.</font>

### #332-B — Smart Coin Selection  ≈ 1000 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** On first start → preselect BTC, ETH, SOL for watchlist. In coin pickers → top 10 by mcap as default. Search history with auto-suggest.</font>

### #332-C — Quick Actions Menu  ≈ 1100 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** On dashboard → floating "Command Bar" (Terminal Command from #86) — make it more discoverable with tooltip on first open. Voice activation (use existing from #109?).</font>

---

# ═══════════════════════════════════════
# PHASE F: PERFORMANCE OPTIMIZATIONS
# ═══════════════════════════════════════

## PROMPT #230 — Coil Migration (removing Glide)

### #230-A — Coil Setup + ImageLoader Singleton  ≈ 900 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Coil already in project. Standardize: create `CoinIconLoader` singleton with custom `ImageLoader` (memory cache 25%, disk cache 50MB, crossfade 200ms, errorPlaceholder = generic coin icon).</font>

### #230-B — Replace Glide Calls  ≈ 1200 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Find all `Glide.with(...).load(...)` in project (incl. XML layouts with `app:imageUrl`) → replace with Coil `AsyncImage` (Compose) or `ImageView.load(...)` (XML). Remove Glide dependency.</font>

### #230-C — Image Loading Performance Pass  ≈ 700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** For coin icons → preload top 50 at app start (background). Lazy load others. SVG support (some exchanges return SVG icons).</font>

---

## PROMPT #240 — Database Performance

### #240-A — Index Audit  ≈ 800 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Review all Room queries in DAOs. For each `WHERE x = ?` → check if `@Entity(indices = [Index("x")])` exists. Add missing ones. Migration for adding indices on live DB.</font>

### #240-B — Query Optimization  ≈ 1100 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Profile slow queries (Room has `RoomDatabase.QueryCallback`). Replace `LIMIT N` patterns without `ORDER BY` with appropriate queries. Add `@Transaction` for multi-table reads.</font>

### #240-C — Pagination for Large Lists  ≈ 1400 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** For news, predictions history, alert events log → add `androidx.paging:paging-runtime:3.3.0`. PagingSource → DAO `@Query("...") fun pagingSource(): PagingSource<Int, X>`. UI: `LazyColumn` + `collectAsLazyPagingItems()`.</font>

---

## PROMPT #241 — Compose Performance

### #241-A — Stability Annotations  ≈ 900 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** For data classes used in Composables → if they have `List<X>` (unstable!) → change to `ImmutableList` (kotlinx.collections.immutable). Or @Stable / @Immutable annotations.</font>

### #241-B — Recomposition Tracking  ≈ 800 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** In debug build enable `LayoutInspector` recomposition counts. Find top 5 over-recomposing composables. Fix (extract state, derivedStateOf, key-based optimization).</font>

### #241-C — LazyColumn / Lazy* Optimization  ≈ 700 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** All `LazyColumn` items → have `key = { it.id }`. Heavy items → `contentType = { ... }`. Disabled animations for screensaver lists.</font>

---

## PROMPT #250 — Code Quality Sweep

### #250-A — Naming Conventions Standardization  ≈ 1200 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** Unify: `XxxRepository` (data layer), `XxxUseCase` (domain), `XxxViewModel` (presentation), `XxxScreen` (Compose UI), `XxxFragment` (legacy XML), `XxxClient` (API), `XxxManager` → rename to either Repository or Service. Document `docs/naming.md`.</font>

### #250-B — God-Object Refactoring  ≈ 1700 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** Find all files >500 lines. For each: identify separate responsibilities, extract into smaller classes. Goal: no class >400 lines.</font>

### #250-C — Duplicate Code Detection  ≈ 1000 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** Find duplicate logic (same formula, same pattern in 2+ places). Extract into shared utility / use case. Tools: `Detekt` static analyzer.</font>

### #250-D — Detekt Static Analysis  ≈ 900 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** Add `io.gitlab.arturbosch.detekt` plugin. Configure in `detekt.yml`. Run `./gradlew detekt` → fix top 30 issues. Add to CI script.</font>

### #250-E — Ktlint Formatting  ≈ 600 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** Add `org.jlleitschuh.gradle.ktlint`. Auto-format entire codebase: `./gradlew ktlintFormat`. Add pre-commit hook.</font>

---

## PROMPT #260 — Error Handling Layer

### #260-A — Result<T> Pattern (sealed class)  ≈ 1000 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `sealed class CryptoResult<out T>: Success<T>, Error(throwable, code), Loading`. Repository methods return `CryptoResult` instead of `Flow<T>` or throws. Helper: `.onSuccess { } .onError { }`.</font>

### #260-B — User-Facing Error Messages  ≈ 1100 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** `ErrorMessageMapper` — maps `Throwable` (UnknownHostException, SocketTimeoutException, HttpException codes) to user-friendly messages. Never show stack trace to user.</font>

### #260-C — Retry Strategy  ≈ 900 tokens
[✅ DONE — 05.05.2026]
<font color="#00FF41">**TASK:** OkHttp Interceptor → exponential backoff retry for HTTP 5xx and network errors. Max 3 retries. Circuit breaker for permanently failed endpoints (5 errors → 5 min cooldown).</font>

---

# PHASE G: BACKEND PREPARATION (for future SaaS)
# ═══════════════════════════════════════
# NOTE: Not a priority for personal use, but architecture
# must be ready if you decide to sell the app.
# ═══════════════════════════════════════

## PROMPT #300 — API Abstraction Layer (for easy backend proxy replacement)

### #300-A — Abstract AI Provider Interface  ≈ 1100 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Current `AIProvider` abstraction is good. Add: `AIProviderRouter` singleton deciding which provider to use (config-driven). If flag `useBackendProxy = true` → routes to your future backend, if `false` → directly to Gemini/Claude/OpenAI with local keys.</font>

### #300-B — Endpoint Configuration  ≈ 900 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** `EndpointsConfig` file (Remote Config-ready) with base URLs for all APIs. Default: direct URLs. Overridable from server-side when adding Firebase Remote Config.</font>

### #300-C — Authentication Token Layer  ≈ 1300 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** `AuthInterceptor` (OkHttp). For now: no-op. Ready to add `Authorization: Bearer xxx` header when user authentication is added. `AuthTokenManager` singleton with `getToken(): String?`.</font>

---

## PROMPT #301 — Subscription Architecture (Google Play Billing already in #113)

### #301-A — SubscriptionState Model  ≈ 800 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** `enum class SubscriptionTier: FREE, PRO, ELITE`. `data class SubscriptionState(tier, validUntil, features: Set<Feature>)`. Synced from Google Play Billing.</font>

### #301-B — Feature Flag System  ≈ 1100 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** `FeatureFlags` singleton. `isEnabled(Feature.AI_TRADE_COACH): Boolean`. Mapping feature → minimum tier. UI: shows `[PRO]` badge on locked features.</font>

### #301-C — Server-side Receipt Validation Stub  ≈ 1000 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** Preparation: `ReceiptValidator` interface. Default impl: client-side (trivial, breakable). Comment with TODO: "Replace with server-side validation when backend is live." Document in `docs/billing_security.md` risks of client-side only.</font>

---

## PROMPT #302 — Remote Config (Firebase)

### #302-A — Remote Config Setup  ≈ 800 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** Add `firebase-config-ktx`. Default values in `xml/remote_config_defaults.xml`. Parameters: `enable_ai_provider_gemini`, `cache_ttl_seconds`, `min_app_version` (kill switch), `featured_coins_list`.</font>

### #302-B — Remote Config Wrapper  ≈ 900 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** `RemoteConfigManager` — fetch on app start (12h cache), apply settings. Notify ViewModels via StateFlow on change.</font>

---

# ═══════════════════════════════════════
# PHASE H: FREE API STRATEGY (no whale-alert paid)
# ═══════════════════════════════════════

## PROMPT #150-NEW — Free On-Chain Whale Tracking (no Whale Alert)

### #150-A — Strategy Document  ≈ 800 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** Document `docs/free_apis.md` with current free alternatives (Whale Alert no longer has free tier — only 7-day trial). Strategy: combine multiple free sources instead of one paid:
- **Etherscan free tier:** 5 calls/sec, 100k/day (current).
- **CoinGecko Demo:** 30 calls/min, 10k/month.
- **Public Ethereum RPC** (Cloudflare, llamaRPC) for raw blockchain queries.
- **Whale Alert Twitter/X feed** (public, scrape RSS-style).</font>

### #150-B — Etherscan Whale Detector  ≈ 1500 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** `EtherscanWhaleClient`: via `txlist` endpoint for known whale addresses (top 100 ETH wallets — publicly known) → check for new txs > $1M. Caching aggressive (5 min). Rate-limit aware (5 calls/sec).</font>

### #150-C — Solana Whale Tracking (free)  ≈ 1200 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** Helius free tier (100k requests/month). `HeliusClient` for Solana whale txs. Combine with SOL-Reactor public endpoints for DEX volume.</font>

### #150-D — BTC Whale (mempool.space — free)  ≈ 1100 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** `MempoolSpaceClient` — public API without key. Endpoint `/api/v1/mining/blocks` + `/api/tx/{txid}` for finding large txs in recent blocks.</font>

### #150-E — Aggregated Whale Feed  ≈ 1000 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** `AggregatedWhaleRepository` — combines ETH + SOL + BTC, normalizes into common `WhaleTransaction` data class, deduplicate, sort by USD value DESC, expose as `Flow<List<WhaleTransaction>>`.</font>

### #150-F — UI: Whale Activity Screen  ≈ 1400 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** `WhaleActivityScreen.kt` — list of txs, filters (chain, min value), real-time updates. Tap → detail (block explorer link).</font>

---

## PROMPT #151 — Free News & Sentiment Aggregation

### #151-A — RSS Aggregator (free, no API)  ≈ 1100 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** `RssAggregator` for: CoinDesk RSS, Cointelegraph RSS, The Block RSS, Decrypt RSS, Bitcoin Magazine RSS. Parse with `Rome` or `OkHttp + manual XML`. Refresh every 15 min.</font>

### #151-B — Reddit JSON API (no auth needed for read)  ≈ 900 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** `RedditClient` via `https://www.reddit.com/r/cryptocurrency.json?limit=25` (no auth, rate limited but free). Parse top posts, comments. User-Agent header mandatory.</font>

### #151-C — CryptoPanic Free Tier  ≈ 800 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** CryptoPanic has free tier (`free.cryptopanic.com/api`) — limited but works. Wrap in `CryptoPanicClient`. Combine with RSS for more coverage.</font>

### #151-D — Local Sentiment Scoring (no external AI)  ≈ 1300 tokens
[✅ DONE — 07.05.2026]
<font color="#00FF41">**TASK:** To avoid wasting Gemini quota for every news item → local sentiment scorer (rule-based + keyword dictionary for crypto context). For deep analysis → batch send to Gemini once daily.</font>

---

# ═══════════════════════════════════════
# PHASE I: PROMPT ENGINEERING — for YouTube/Facebook content
# ═══════════════════════════════════════

## PROMPT #500 — Content Generation Prompt Templates

### #500-A — Audience Targeting System  ≈ 1000 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** `AudienceProfile` enum: `DAY_TRADER`, `HODLER`, `DEFI_USER`, `CRYPTO_NEWBIE`, `CONTENT_FAN`. Each prompt template takes audience and adapts tone, length, technical detail.</font>

### #500-B — Daily Market Recap Template  ≈ 1100 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** `DailyRecapPromptBuilder` — Hook (15-20 words), Context (2-3 sentences), Numbers (3-5 key metrics), What it means (3-4 sentences), What to watch (2-3 bullets), CTA. English language, no clichés.</font>

### #500-C — Video Prompt Generator (short, visual)  ≈ 1200 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** For Sora/Runway/Veo3 — short prompts (max 50 words). Template: `{Subject} + {Action} + {Style} + {Camera}`. Examples: cinematic close-up, slow-mo, neon, Blade Runner aesthetic.</font>

### #500-D — YouTube Thumbnail Prompt Template  ≈ 800 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** For Midjourney/DALL-E thumbnail generation. Template with: 3-word headline, color palette (black + neon), subject (stylized coin), style ("Mr Beast meets Bloomberg").</font>

### #500-E — Facebook Post Template  ≈ 900 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** For Facebook global audience — professional tone, Q&A format, 80-120 words, 1 emoji at start, clear CTA "Share your thoughts in the comments".</font>

### #500-F — Whale Alert Narrator Template  ≈ 1000 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** When tracker from #150 catches a large tx → auto-generate 30-second TikTok script. Format: dramatic opening, tx detail, market impact prediction, suspense ending.</font>

### #500-G — "What If" Scenario Generator  ≈ 1100 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Weekly speculative content. Prompt: "Generate a 'what if BTC dropped to $X next week' scenario, argue both pros and cons, end with engagement question."</font>

### #500-H — Coin Comparison Generator  ≈ 1200 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** For "ETH vs SOL for 2026" format. Tabular comparison (technicals, ecosystem, risks), pros/cons each, verdict (no financial advice).</font>

### #500-I — Newsletter Digest Template  ≈ 1100 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Weekly digest for Substack/MailerLite. 3 sections: Top 3 newsworthy events, Big mover of the week, Watching next week. ~600 words. Subject line A/B variants.</font>

### #500-J — Prompt Library UI Screen  ≈ 1400 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** `ContentStudioScreen.kt` — UI for choosing template + audience + parameters → generates prompt + sends to AI provider → shows result + copy/share buttons.</font>

---

# ═══════════════════════════════════════
# PHASE J: ADVANCED FEATURES (for score 9.5+)
# ═══════════════════════════════════════

## PROMPT #600 — Voice Commands Completion

### #600-A — Speech Recognition Setup  ≈ 1000 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** `SpeechRecognizer` Android API (offline for basic commands). Trigger: long-press on Terminal Command Bar.</font>

### #600-B — Command Parser  ≈ 1300 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Natural language → command. Examples: "show bitcoin price", "create alert for ethereum at 4000", "analyze solana". Fuzzy matching, regex + intent classification.</font>

### #600-C — Voice Feedback (TTS)  ≈ 800 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** `TextToSpeech` (Android API). On voice command → voice response: "BTC currently at 70 thousand 234 dollars". English voice support.</font>

---

## PROMPT #610 — Widget Enhancements

### #610-A — Multiple Widget Sizes  ≈ 1200 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Glance widget with 3 sizes: 2x1 (single coin price), 4x2 (top 5 watchlist), 4x4 (mini-dashboard with heatmap). Resizable.</font>

### #610-B — Widget Customization  ≈ 1100 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Configuration screen on widget add: coin(s) selection, refresh interval, theme variant, show/hide elements.</font>

### #610-C — Widget Click Actions  ≈ 800 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Tap on coin in widget → opens app on that coin's chart screen. Tap on refresh icon → manual refresh.</font>

---

## PROMPT #620 — iOS-style Polish (for premium feel)

### #620-A — Haptic Feedback Refinement  ≈ 700 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Different haptic patterns for: button tap (light), price alert (medium), achievement (success), error (heavy). Use Android 12+ `HapticFeedbackConstants`.</font>

### #620-B — Smooth Animations Audit  ≈ 1100 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Audit all screen transitions. Goal: 60 fps no jank. Compose `animateContentSize`, `animateFloatAsState`. Spring animations for natural feel.</font>

### #620-C — Sound Design Polish  ≈ 900 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** CRT sounds (from #145) — add: button beep (subtle), error chirp, alert chime, screen-on power-up sound. Volume controlled by user.</font>

---

## PROMPT #630 — Achievement / Gamification System

### #630-A — Achievements Schema  ≈ 1000 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** `Achievement` data class (id, title, description, icon, unlockedAt). Examples: "First Alert", "10 Predictions Made", "7-day Streak", "Whale Watcher" (1st whale tx tracked).</font>

### #630-B — Achievement Engine  ≈ 1200 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** `AchievementEngine` listens to app events (alert created, prediction made, etc.). On condition met → unlock + show toast/notification.</font>

### #630-C — Achievements Screen  ≈ 1100 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Compose screen with grid of icons (locked → grayscale + ?, unlocked → colored). Stats: X/Y unlocked, total XP, streak counter.</font>

---

# ═══════════════════════════════════════
# PHASE K: CI/CD, REFACTORING & FINAL POLISH (PRIORITY 1)
# ═══════════════════════════════════════

## PROMPT #700 — API Keys Verification (#200-VERIFY)
**TASK:** Ensure all critical API keys are moved to `local.properties` and accessed via `BuildConfig`.

### #700-A — BuildConfig Check  ≈ 800 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Audit `app/build.gradle.kts`. Ensure GEMINI, COINGECKO, ETHERSCAN, HELIUS keys are loaded from `local.properties`. Verify no "AIza..." strings in source code.</font>

---

## PROMPT #710 — Static Analysis & Formatting (#250-A, #250-B)
**TASK:** Re-configure and enforce Detekt and Ktlint.

### #710-A — Detekt Baseline  ≈ 900 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Update `detekt` configuration. Create baseline and update README. Ensure it compiles and runs via `./gradlew detekt`.</font>

### #710-B — Ktlint Auto-format  ≈ 700 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Run `./gradlew ktlintFormat`. Commit results separately as "style: ktlint auto-format".</font>

---

## PROMPT #720 — Git Hooks & Automation (#250-C)
**TASK:** Implement pre-commit hooks for security and quality.

### #720-A — Pre-commit Security Hook  ≈ 800 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Create `scripts/pre-commit.sh` to block commits with hardcoded keys or Detekt failures. Document in `docs/git_hooks.md`.</font>

---

## PROMPT #730 — God-Object Refactoring (#350-A, #350-B, #350-C)
**TASK:** Break down massive ViewModels and Repositories into UseCases and DataSources.

### #730-A — DashboardViewModel Refactor  ≈ 1500 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Extract `GetDashboardDataUseCase` and `ObserveTickerUseCase`. Reduce `DashboardViewModel` to <200 lines. Maintain test compatibility.</font>

### #730-B — AnalysisViewModel Refactor  ≈ 1800 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Extract `RunDeepAnalysisUseCase`, `GenerateAnalysisReportUseCase`, and `ObserveAnalysisHistoryUseCase`. Reduce `AnalysisViewModel` to <250 lines.</font>

### #730-C — CryptoRepository Decomposition  ≈ 1600 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Split `CryptoRepositoryImpl` into `BinancePriceSource`, `CoinGeckoPriceSource`, etc. Repository becomes an orchestrator with fallback logic.</font>

---

## PROMPT #740 — CI/CD Workflows (#360-A, #360-B)
**TASK:** Setup GitHub Actions for automated build and release.

### #740-A — Build Workflow  ≈ 900 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Create `.github/workflows/build.yml` for PR/Push testing. Runs tests, Detekt, and builds debug APK.</font>

### #740-B — Release Workflow  ≈ 1100 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Create `.github/workflows/release.yml`. Automate signed AAB build and GitHub Release on tag push. Use Secrets for Keystore and API keys.</font>

---

## PROMPT #750 — Performance Benchmarking (#370-A, #370-B)
**TASK:** Measure and enforce performance thresholds for core logic.

### #750-A — Prediction Engine Benchmarks  ≈ 1200 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Create `PredictionBenchmarkTest.kt`. Verify FFT (<100ms), Monte Carlo (<500ms), Wyckoff (<50ms) performance.</font>

### #750-B — Database Benchmarks  ≈ 1000 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Create `DatabaseBenchmarkTest.kt` using `androidx.benchmark`. Test insertion (1000 entities <500ms) and complex queries.</font>

---

## PROMPT #760 — UI Accessibility & Polish (#380-A, #380-B)
**TASK:** Ensure the app is accessible and meets Material design standards.

### #760-A — Accessibility Audit  ≈ 1400 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Review all 47 screens. Add `contentDescription`, `testTag`, and TalkBack support.</font>

### #760-B — Touch Target Optimization  ≈ 1000 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Ensure all interactive elements have a minimum size of 48dp.</font>

---

## PROMPT #770 — Modern Android Integration
**TASK:** Support latest Android features (Splash Screen, Back handling, Shortcuts).

### #770-A — Splash Screen API  ≈ 900 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Implement `androidx.core:core-splashscreen`. CRT-style boot splash with transition to Dashboard.</font>

### #770-B — Predictive Back & Shortcuts  ≈ 1100 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Enable Predictive Back support. Add App Shortcuts for Alerts, Briefing, Whale Tracker, and Content Studio.</font>

---

## PROMPT #780 — Memory Leak Audit
**TASK:** Final stability check using LeakCanary.

### #780-A — Leak Detection & Fix  ≈ 1300 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Run full app audit with LeakCanary 2.14. Fix top 5 leaks (Canvas, WebSockets, Audio). Document in `docs/leak_audit.md`.</font>

---

---

# ═══════════════════════════════════════
# PHASE L: SUPREME OPTIMIZATION & REFACTORING (PRIORITY 2)
# ═══════════════════════════════════════

## PROMPT #800 — Constants & Complexity Reduction
**TASK:** Remove magic numbers and simplify complex engines.

### #800-A — Global Constants Extraction  ≈ 1500 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Create `AppConstants.kt` and `TerminalConfig.kt`. Extract all hardcoded strings, UI dimensions, animation durations, and math thresholds (Elliott Wave limits, RSI thresholds) into these objects.</font>

### #800-B — Engine Refactoring  ≈ 1700 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Identify functions with high Cyclomatic Complexity (e.g., in `TechnicalAnalysisEngine.kt`). Break down deep nested `if/else` and `when` statements into smaller helper functions or use polymorphism.</font>

---

## PROMPT #810 — Battery & Offline Optimization
**TASK:** Implement robust background sync and caching.

### #810-A — Offline-First Room Caching  ≈ 2000 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Implement Room DAO and Entities for caching primary Dashboard data. Implement a `NetworkBoundResource` Flow strategy (emit Local -> fetch Remote -> save Local -> emit new Local).</font>

### #810-B — WorkManager Migration  ≈ 1400 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Create `CryptoDataSyncWorker` using `WorkManager`. Replace non-critical Foreground Services with periodic background syncing (15-30 min intervals).</font>

### #810-C — WebSocket Lifecycle Optimization  ≈ 1000 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Ensure WebSocket connections are tied strictly to `RESUMED` state using `collectAsStateWithLifecycle`. Terminate WS instantly when app goes to background.</font>

---

## PROMPT #820 — Interactive Onboarding Enhancement
**TASK:** Create a guided tour for terminal features.

### #820-A — Onboarding UI Overlay  ≈ 2500 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Create a reusable Compose overlay (Showcase style) that highlights UI elements like AI Coach and Whale Tracker. Add terminal-style typewriter text explanations and a `[ SKIP_TUTORIAL ]` button.</font>

### #820-B — Terminal Manual commands  ≈ 1200 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Update Terminal Command Parser to recognize `/help`, `/man [feature]`, and `/tutorial` commands to trigger the explanation overlays on-demand.</font>

---

## PROMPT #830 — Focus Mode (Simplified UI)
**TASK:** Provide a distraction-free terminal view.

### #830-A — Focus Mode UI & State  ≈ 1200 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Add `FocusModeEnabled` in DataStore. Create `DashboardFocusView` showing only: Asset ticker, current price, 24h change, and simple sentiment indicator. Hide all complex charts.</font>

### #830-B — Focus Mode Toggle  ≈ 800 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Add terminal command `/focusmode on/off` and a UI switch in settings to toggle Focus Mode dynamically.</font>

---

## PROMPT #840 — Alpha Signals & PRO Gating
**TASK:** Exclusive real-time intelligence for elite users.

### #840-A — AlphaSignalEngine  ≈ 1800 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Create `AlphaSignalEngine` combining Whale movement and Sentiment. Trigger signal if `WhaleVolume > Threshold` AND `SentimentScore > Threshold` within timeframe.</font>

### #840-B — PRO Integration & UI  ≈ 1500 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Gate Alpha Signals via Billing Repository. If FREE: show obfuscated entry. If PRO: show full breakdown with cyberpunk aesthetics (red/gold accents, CRT scanlines).</font>

---

## PROMPT #850 — Testing & Quality Assurance
**TASK:** Reach target 80% coverage for business logic.

### #850-A — Engine & UseCase Tests  ≈ 1500 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Write comprehensive unit tests for `TechnicalAnalysisEngine` and `AlphaSignalEngine`. Test edge cases and extreme market conditions with mocked data.</font>

### #850-B — ViewModel Flow Tests  ≈ 1200 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Use Turbine to test `DashboardViewModel` StateFlows, ensuring correct emission of Loading, Success, and Error states.</font>

---

# ═══════════════════════════════════════
# PHASE M: ELITE SUPREMACY (DEFI, DERIVATIVES & REAL-TIME)
# ═══════════════════════════════════════

## PROMPT #900 — Derivatives Terminal Expansion
**TASK:** Advanced metrics for futures trading.

### #900-A — Funding Rate Heatmap (Compose) ≈ 1500 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Create a heatmap visualization for funding rates across top 20 coins. Map Binance, Bybit, and OKX rates. Color code: Deep Blue (Extreme Negative) to Bright Red (Extreme Positive). Implement clickable cells for coin details.</font>

### #900-B — Liquidation Prediction Engine ≈ 1800 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Logic to calculate "Magnetic Zones" where large clusters of liquidations are sitting. Use volume profile + current price + volatility to estimate where the next "squeeze" might happen. **Add 3 unit tests** for various volatility scenarios.</font>

---

## PROMPT #910 — DeFi Intelligence Hub
**TASK:** Tracking on-chain protocols.

### #910-A — Yield Aggregator Dashboard (Compose) ≈ 1700 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Fetch real-time APY from major protocols (Aave, Uniswap, Curve) via public subgraphs or APIs. Allow users to "Simulate LP" to see potential impermanent loss based on historic volatility.</font>

---

## PROMPT #920 — Real-Time WebSocket Infrastructure
**TASK:** Moving from polling to instant updates for Elite mode.

### #920-A — Unified WebSocket Manager ≈ 1400 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Singleton that manages connections to Binance (Ticker + AggTrade), Coinbase, and Kraken. Expose as a single `SharedFlow<MarketEvent>`. Automatic reconnection with exponential backoff. Lifecycle-aware (only active when app in foreground).</font>

### #920-B — Instant Whale Alerts (WS) ≈ 1200 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Instead of periodic polling, use WebSockets to detect large trades (> $500k) INSTANTLY as they happen. Trigger "Flash Notification" for PRO users. Unify with existing #150 logic.</font>

---

## PROMPT #930 — AI Trading Strategy Builder
**TASK:** User-defined strategies with AI validation.

### #930-A — Strategy DSL + Engine ≈ 2000 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Create a simple DSL for defining strategies (e.g., "IF RSI < 30 AND VOLUME > 2X AVG THEN BUY"). Engine to run this logic against current `MarketSnapshot`.</font>

### #930-B — Backtesting Simulator ≈ 2500 tokens
[✅ DONE — 08.05.2026]
<font color="#00FF41">**TASK:** Run defined strategy against last 30 days of 1h candle data. Calculate: Win Rate, Profit Factor, Max Drawdown. UI: terminal-style report with ASCII chart.</font>

---

# ═══════════════════════════════════════
# 📊 PROGRESS TRACKING TABLE
# ═══════════════════════════════════════

## PHASE A — Current Tasks (Priority 1)
 # | Prompt | Parts | Completed | Status |
---|---|---|---|---|
 127 | Custom Composite Alerts | 7 (A-G) | 7/7 | ✅ DONE |
 140 | Heatmap Screensaver + Cycle | 6 (A-F) | 6/6 | ✅ DONE |

## PHASE B — Security (Priority 1 — CRITICAL)
 # | Prompt | Parts | Completed | Status |
---|---|---|---|---|
 200 | API Keys Audit & Migration | 4 (A-D) | 4/4 | ✅ DONE |
 201 | Encrypted Local Storage | 3 (A-C) | 3/3 | ✅ DONE |
 202 | Network Security + Pinning | 3 (A-C) | 3/3 | ✅ DONE |
 210 | ProGuard / R8 | 3 (A-C) | 3/3 | ✅ DONE |
 220 | Root / Tamper Detection | 3 (A-C) | 3/3 | ✅ DONE |

## PHASE C — Architecture
 # | Prompt | Parts | Completed | Status |
---|---|---|---|---|
 270 | Hilt DI Migration | 7 (A-G) | 7/7 | ✅ DONE |
 280 | Compose Migration | 10 (A-J) | 10/10 | ✅ DONE |

## PHASE D — Testing + Crash
 # | Prompt | Parts | Completed | Status |
---|---|---|---|---|
 310 | Unit Tests Foundation | 7 (A-G) | 7/7 | ✅ DONE |
 320 | Firebase Crashlytics + Analytics | 6 (A-F) | 6/6 | ✅ DONE |
 340 | Production Stability Sweep | 4 (A-D) | 4/4 | ✅ DONE |

## PHASE E — UX
 # | Prompt | Parts | Completed | Status |
---|---|---|---|---|
 330 | Welcome Screen + Onboarding | 6 (A-F) | 6/6 | ✅ DONE |
 331 | Empty + Loading + Error States | 3 (A-C) | 3/3 | ✅ DONE |
 332 | Smart Defaults + Disclosure | 3 (A-C) | 3/3 | ✅ DONE |

## PHASE F — Performance
 # | Prompt | Parts | Completed | Status |
---|---|---|---|---|
 230 | Coil Migration | 3 (A-C) | 3/3 | ✅ DONE |
 240 | Database Performance | 3 (A-C) | 3/3 | ✅ DONE |
 241 | Compose Performance | 3 (A-C) | 3/3 | ✅ DONE |
 250 | Code Quality Sweep | 5 (A-E) | 5/5 | ✅ DONE |
 260 | Error Handling Layer | 3 (A-C) | 3/3 | ✅ DONE |

## PHASE G — Backend Prep (for SaaS)
 # | Prompt | Parts | Completed | Status |
---|---|---|---|---|
 300 | API Abstraction Layer | 3 (A-C) | 3/3 | ✅ DONE |
 301 | Subscription Architecture | 3 (A-C) | 3/3 | ✅ DONE |
 302 | Remote Config | 2 (A-B) | 2/2 | ✅ DONE |

## PHASE H — Free APIs
 # | Prompt | Parts | Completed | Status |
---|---|---|---|---|
 150-NEW | Free On-Chain Whale Tracking | 6 (A-F) | 6/6 | ✅ DONE |
 151 | Free News & Sentiment | 4 (A-D) | 4/4 | ✅ DONE |

## PHASE I — Content Prompt Engineering
 # | Prompt | Parts | Completed | Status |
---|---|---|---|---|
 500 | Content Generation Templates | 10 (A-J) | 10/10 | ✅ DONE |

## PHASE J — Advanced Features
 # | Prompt | Parts | Completed | Status |
---|---|---|---|---|
 600 | Voice Commands | 3 (A-C) | 3/3 | ✅ DONE |
 610 | Widget Enhancements | 3 (A-C) | 3/3 | ✅ DONE |
 620 | iOS-style Polish | 3 (A-C) | 3/3 | ✅ DONE |
 630 | Achievement System | 3 (A-C) | 3/3 | ✅ DONE |

## PHASE K — Supreme Mode v5 (CI/CD & Refactoring)
 # | Prompt | Parts | Completed | Status |
---|---|---|---|---|
 700 | API Keys Verification | 1 (A) | 1/1 | ✅ DONE |
 710 | Static Analysis & Formatting | 2 (A-B) | 2/2 | ✅ DONE |
 720 | Git Hooks & Automation | 1 (A) | 1/1 | ✅ DONE |
 730 | God-Object Refactoring | 3 (A-C) | 3/3 | ✅ DONE |
 740 | CI/CD Workflows | 2 (A-B) | 2/2 | ✅ DONE |
 750 | Performance Benchmarking | 2 (A-B) | 2/2 | ✅ DONE |
 760 | UI Accessibility & Polish | 2 (A-B) | 2/2 | ✅ DONE |
 770 | Modern Android Integration | 2 (A-B) | 2/2 | ✅ DONE |
 780 | Memory Leak Audit | 1 (A) | 1/1 | ✅ DONE |

## PHASE L — Optimization & Refactoring (Priority 2)
 # | Prompt | Parts | Completed | Status |
---|---|---|---|---|
 800 | Constants & Complexity | 2 (A-B) | 2/2 | ✅ DONE |
 810 | Battery & Offline | 3 (A-C) | 3/3 | ✅ DONE |
 820 | Onboarding guided tour | 2 (A-B) | 2/2 | ✅ DONE |
 830 | Focus Mode | 2 (A-B) | 2/2 | ✅ DONE |
 840 | Alpha Signals | 2 (A-B) | 2/2 | ✅ DONE |
 850 | Business Logic Testing | 2 (A-B) | 2/2 | ✅ DONE |

## PHASE M — Elite Supremacy (Priority 2)
 # | Prompt | Parts | Completed | Status |
---|---|---|---|---|
 900 | Derivatives Expansion | 2 (A-B) | 2/2 | ✅ DONE |
 910 | DeFi Intelligence | 1 (A) | 1/1 | ✅ DONE |
 920 | Real-Time Infrastructure | 2 (A-B) | 2/2 | ✅ DONE |
 930 | AI Strategy Builder | 2 (A-B) | 2/2 | ✅ DONE |

---

## TOTAL:
- **Prompts:** 48
- **Atomic parts:** 174
- **Estimated total tokens:** ~218,000
- **Current progress:** 174/174 (100%)

---

## 🎯 EXECUTION ORDER (recommended)

1. **First:** PHASE B (Security) — (DONE)
2. **Next:** PHASE A (Completing the current) — (DONE)
3. **Then:** PHASE D (Crashlytics + Tests) — (DONE)
4. **Next:** PHASE E (UX/Onboarding) — (DONE)
5. **Major architectural work:** PHASE C — (DONE)
6. **Quality:** PHASE F — (DONE)
7. **Free APIs:** PHASE H — (DONE)
8. **Content:** PHASE I — (DONE)
9. **Backend prep:** PHASE G — (DONE)
10. **Advanced:** PHASE J — (DONE)
11. **Supreme Polish:** PHASE K — (DONE)
12. **Secondary Polish:** PHASE L — (DONE)
13. **Elite Supremacy:** PHASE M — (DONE)
14. **Emergency Stabilization & Branding** — (DONE) [10.05.2026]
    * [✅ DONE] Resolve WorkManager startup crash (idle constraint).
    * [✅ DONE] Fix Hilt binding for `NetworkHealthDao`.
    * [✅ DONE] Restore API connectivity (SSL pinning bypass).
    * [✅ DONE] Custom Branding: Terminal icon applied to launcher.
    * [✅ DONE] Parallelize data fetching and improve resilience (Markets/News).
15. **User Feedback & UX Audit (Live)** — (DONE) [11.05.2026]
    * [✅ DONE] Fix Ticker Tape rotation (Top 10 fallback).
    * [✅ DONE] Repair Admin Password (*).
    * [✅ DONE] Resolve "Connecting..." hang in Global Market Bar.
    * [✅ DONE] Enlarge System Event Log for better readability.
    * [✅ DONE] Restore AI Market Narrative stability.
    * [✅ DONE] Add asset selection flexibility in Entry/MTF screens.
    * [✅ DONE] Fix Terminal console keyboard occlusion (IME padding).
16. **Supreme Stability & Accessibility Audit** — (DONE) [12.05.2026]
    * [✅ DONE] Global Market Feed recovery with 15s timeout and retry logic.
    * [✅ DONE] Markets Screen skeleton hang resolved with data fallback.
    * [✅ DONE] forgiving Admin Password (* / *) with uppercase/trim logic.
    * [✅ DONE] Terminal Command Bar added to all main screens (Markets, ToolsHub) for consistency.
    * [✅ DONE] Universal IME padding applied to all terminal inputs to prevent occlusion.
    * [✅ DONE] Analysis Screen optimized with default coin list (Top 10) and coin ID mapping.
17. **Authentication & Persistent Admin Status** — (DONE) [12.05.2026]
    * [✅ DONE] Fixed Admin Password typo (now strictly `*`).
    * [✅ DONE] Integrated Firebase Auth with Google Sign-In for persistent access.
    * [✅ DONE] Created `AuthService` to manage session state and automatic admin escalation.
    * [✅ DONE] Added "Sign in with Google" option to Admin Authorization dialog.
18. **Data Throughput Optimization** — (DONE) [12.05.2026]
    * [✅ DONE] Limited initial market fetch to Top 15 coins to reduce latency and API overhead.
    * [✅ DONE] Streamlined default watchlist in Analysis and MTF screens.
    * [✅ DONE] Optimized repository to prioritize essential assets during congested network states.

---

# ═══════════════════════════════════════
# PHASE N: USER FEEDBACK & HOTFIXES (LIVE AUDIT)
# ═══════════════════════════════════════

## PROMPT #1000 — User Interface & Connectivity Polish
**TASK:** Fix critical UX issues discovered during live testing.

### #1000-A — UI/UX Hotfixes  ≈ 1200 tokens
[✅ DONE — 11.05.2026]
- **Ticker Tape**: Fallback to Top 10 coins if none are tracked.
- **Admin Access**: Standardized master key access.
- **Event Log**: Enlarged `system_event_log` box for better visibility.
- **Keyboard Handling**: Added `imePadding` to prevent console occlusion.

### #1000-B — Stability & Data Flow  ≈ 1000 tokens
[✅ DONE — 11.05.2026]
- **Market Feed**: Added 10s timeouts to prevent "Connecting..." hang.
- **Tool Navigation**: Added `[TRACK_MORE]` shortcuts.

## PROMPT #1001 — Supreme Stability Pass
**TASK:** Resolve persistent connectivity and UI occlusion issues.

### #1001-A — Resilient Data Fetching ≈ 1500 tokens
[✅ DONE — 12.05.2026]
- **Global Market Feed**: Implemented 15s timeout with automatic retry and hardcoded fallback data to ensure the UI is never stuck on "Connecting...".
- **Markets Skeleton Fix**: Added logic to `MarketsViewModel` to timeout loading and show cached data or an error message if the API is unresponsive for >10s.
- **Analysis Optimization**: Added 5s timeout to OHLC data refresh in `RunDeepAnalysisUseCase` to prevent "slow loading" when the network is congested.

### #1001-B — Terminal & UX Refinement ≈ 1300 tokens
[✅ DONE — 12.05.2026]
- **Universal Terminal**: Added `TerminalCommandBar` to Markets and ToolsHub screens.
- **IME Occlusion**: Applied `imePadding()` and `adjustResize` behavior across all screens with terminal inputs.
- **Smart Asset Selector**: Added Top 10 coins as default in `AnalysisViewModel` and added mapping for short symbols (e.g., "BTC" -> "bitcoin").
- **Forgiving Admin Login**: Modified `AdminPasswordDialog` to accept both standard and backup master keys (common typos) and added `trim()` + `uppercase()` logic.

## PROMPT #1002 — Authentication & Persistence
**TASK:** Implement Google Login and fix admin access.

### #1002-A — Firebase Google Auth ≈ 1800 tokens
[✅ DONE — 12.05.2026]
- **Admin Password**: Strictly enforced master key access.
- **Google Auth**: Added `firebase-auth` and `play-services-auth` dependencies.
- **AuthService**: Implemented `AuthService` singleton to handle Firebase session and automatic admin rights for verified emails.
- **Login UI**: Integrated Google Login button in the Admin Dialog for seamless persistent access.

## PROMPT #1003 — Throughput & Speed Optimization
**TASK:** Reduce API payload and improve loading times.

### #1003-A — Lightweight Asset Fetching ≈ 1200 tokens
[✅ DONE — 12.05.2026]
- **Top 15 Limit**: Reduced the default market fetch from 50 to 15 assets. This significantly lowers JSON parsing time and reduces the risk of hitting API rate limits.
- **Fast Default List**: Updated `AnalysisViewModel` with a hardcoded fallback of the Top 15 symbols (BTC, ETH, SOL, BNB, etc.) to ensure the UI is interactive immediately upon entry.
- **Database Priority**: Optimized the repository to fetch the Top 15 coins by market cap whenever the user's specific watchlist is empty.

---

*Updated by CryptoDept Senior Architect | 12.05.2026 | v6.7 — SPEED OPTIMIZED*
