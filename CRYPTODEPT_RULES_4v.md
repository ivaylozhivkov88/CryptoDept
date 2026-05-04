# CryptoDept — Мастър промпт за Gemini Agent
## Версия: SUPREME MODE (v3.0) — Пътят към съвършенство

---

## ЗАДЪЛЖИТЕЛНИ ПРАВИЛА ЗА АГЕНТА — ПРОЧЕТИ ВЕДНЪЖ, СЛЕДВАЙ ВИНАГИ

### Режим на комуникация — СТРОГО
- Отговаряй САМО с код и минимален коментар
- БЕЗ обяснения как работи кодът освен ако не питам
- БЕЗ увод, БЕЗ заключение, БЕЗ "Ето как...", БЕЗ "Страхотно!"
- При грешка: 1 ред причина + fix. Нищо повече.
- При завършена стъпка: само `[DONE] #N — filename.kt`
- **МАСТЪРПЛАН:** Веднага след изпълнение на промпт, той трябва да бъде изтрит от `CRYPTODEPT_MASTERPLAN_v4.md`.
- **ТОКЕН МЕНИДЖМЪНТ:** Преди старт на нов промпт задължително провери нужните токени спрямо текущия ти лимит/квота. Ако са недостатъчни или близо до лимита, спри и съветвай за изчакване. Чакай потвърждение за старт.
- При въпрос към мен: максимум 1 изречение
- Код блоковете да са пълни и готови за copy-paste
- НЕ повтаряй код който вече съществува — само новото/променeното
- Използвай `// ...existing code...` за да прескочиш непроменени части

### Формат на работния лог
```
[DONE] #N filename.kt
[NEXT] #N+1 filename.kt
```

---

## КОНТЕКСТ — НЕ ОБЯСНЯВАЙ, САМО ПРОЧЕТИ

- Проект: CryptoDept Android (Kotlin + Compose + Hilt + Room)
- Тема: "Wall Street Terminal 90s" — черен фон, зелен (#00FF41) и кехлибарен (#FFB000)
- Шрифт: JetBrains Mono навсякъде за данни
- Архитектура: Clean Architecture + MVVM + Repository + UseCases
- DI: Hilt 2.53 | DB: Room v3 (migration 1→2→3 документирана) | Prefs: DataStore
- API: CoinGecko, Binance WebSocket, CoinGlass, Etherscan, Alpha Vantage
- AI: Gemini API (gemini-1.5-flash модел)
- Firebase: Crashlytics + Remote Config
- v1 завършена, v2 завършена, v3 = Supreme Mode — всичко трябва да работи перфектно

---

## 🎨 ВИЗУАЛЕН СТАНДАРТ v3 — АБСОЛЮТЕН

### Забранени елементи — НИКОГА
- ❌ borderRadius > 2dp (само квадратни ъгли)
- ❌ Dynamic Material You / system colors
- ❌ Sans-serif шрифтове за данни и числа
- ❌ Светли фонове (освен White Mode)
- ❌ Shadow / elevation / gradient
- ❌ Анимации > 600ms
- ❌ Hardcoded цветове като `Color(0xFF00FF41)` в UI компоненти — САМО `LocalTerminalColors.current`
- ❌ `CircularProgressIndicator` — замени`TerminalLoadingSkeleton`
- ❌ `CRTBlack` директно в Composable — само `colors.background`

### Задължителни елементи — ВИНАГИ
- ✅ `LocalTerminalColors.current` за ВСИЧКИ цветове в UI
- ✅ JetBrains Mono / FontFamily.Monospace за всички данни
- ✅ Квадратни ъгли — `RectangleShape` или `shape = RectangleShape`
- ✅ Border 1dp `colors.grid` на всички карти
- ✅ Padding 12dp навсякъде
- ✅ CRTOverlay на всички екрани (оптимизиран — без recomposition)
- ✅ `[LABEL]` формат за всички бутони
- ✅ `>>>` prefix за заглавия на секции
- ✅ Всички числа форматирани с `Locale.US`

### 📝 ТЕКСТОВИ РЕПОРТИ (NARRative AI STYLE) — СТАНДАРТ
- **Структура:** "Crypto Apostles" стил (от Снимка 1).
- **Заглавие:** Гръмко, с емоджита (напр. 🚀 BTC AT $100K: EXPLOSION OR TRAP? 🚀).
- **Увод:** Провокативен, спиращ скролването ("Stop scrolling...").
- **Секции:** Описателни имена с ➡️ (напр. ➡️ THE BEAR TRAP, ➡️ THE DISBELIEF RALLY).
- **Прогноза:** Конкретни нива и таймфрейм (напр. WEEKEND PREDICTION).
- **Присъда:** "The CryptoDept Verdict" — финално мнение.
- **Реклама:** Интегрирано споменаване на терминала като елитен инструмент.
- **Engagement:** Подкана за коментар/емоджи в края + Hashtags.
- **Тон:** Професионален сторителинг, "Smart Money" перспектива.

### Терминален език (UI strings)
- Зареждане: `LOADING DATA...` / `FETCHING MARKET DATA...`
- Грешка: `[ERROR] CONNECTION FAILED`
- Успех: `[OK] DATA SYNCHRONIZED`
- Празно: `NO DATA AVAILABLE`

---

## 🏗️ АРХИТЕКТУРНИ ПРАВИЛА v3

### Слоеве — СТРИКТНО
```
domain/model/       → само data classes, sealed classes, enums. БЕЗ Android imports
domain/repository/  → само interfaces
domain/usecase/     → бизнес логика, suspend fun, coroutines
data/api/           → Retrofit interfaces, WebSocket
data/db/            → Room entities, DAOs, TypeConverters
data/repository/    → имплементации
data/datastore/     → PreferencesManager
di/                 → Hilt modules
viewmodel/          → StateFlow, SharedFlow, hiltViewModel
ui/                 → САМО Composable функции и UI логика
service/            → ForegroundService, WorkManager workers
util/               → helper functions, constants
```

### Правила за ViewModels
- Само `StateFlow` и `SharedFlow` — никакви `LiveData`
- `viewModelScope.launch` с `Dispatchers.IO` за мрежа/DB
- `Dispatchers.Default` за изчисления (prediction engines)
- UiState sealed class за всеки ViewModel: `Loading | Success(data) | Error(msg)`
- `catch { }` с `emit(Error(...))` на всички flow колектори

### Правила за Use Cases
- Всеки UseCase = отделен клас, `@Singleton` с `@Inject constructor`
- `suspend fun invoke(...)` или `fun invoke(...): Flow<...>`
- Изчислително-тежки операции: `withContext(Dispatchers.Default) { }`
- Резултати винаги кешират се с `mutableStateOf` в Repository или `cachedIn`

### Правила за Repository
- Само Repository знае за Room + API — никой друг
- WebSocket данни → `MutableStateFlow` в Repository → exposed като `StateFlow`
- Никога не хвърляй exceptions навън — wrap в `Result<T>` или sealed class

---

## 🔒 СИГУРНОСТ — ЗАДЪЛЖИТЕЛНО

- API ключове САМО в `secrets.properties` (gitignored) → BuildConfig полета
- `secrets.properties` е в `.gitignore` — НИКОГА не се commit-ва
- BuildConfig полета: `COINGECKO_API_KEY`, `BINANCE_API_KEY` и т.н.
- `local.properties` НИКОГА не съдържа API ключове

---

## ⚡ ПРОИЗВОДИТЕЛНОСТ — ЗАДЪЛЖИТЕЛНО

- `FourierCyclePredictor`: изчислението е `withContext(Dispatchers.Default)` + резултатът се кешира за 5 минути
- `CRTOverlay`: `remember { }` за всички paint обекти, `drawWithCache` за scanlines
- `TickerTape`: `key(item.id)` в `LazyRow items { }` за stable keys
- Prediction Engine резултати: кешират се в `ViewModel` с `stateIn(viewModelScope)`
- Room queries: добавени `@Index` на `coinId` и `timestamp` колони
- Мрежови заявки: `OkHttp` с `Cache` 10MB за REST (не WebSocket)

## 🔧 TOKEN EFFICIENCY RULES (Агент) — КРИТИЧНО

**QUANDO РАБОТИШ С ОГРАНИЧЕНИ ТОКЕНИ:**
- ✅ Чети файлове САМО при първи контакт, после използвай cache
- ✅ Batch операции: направи 3-5 промени в един chat
- ✅ Използвай `// ...existing code...` за пропускане на непроменени части
- ✅ Без обяснения efter code - САМО резултат `[DONE]`
- ✅ grep_search + replace_string_in_file вместо read_file за малки промени
- ✅ Focus на критични файлове - пропускай comments/docs
- ✅ Не правиться повторни queries - събери contexthash първо
- ✅ При build errors: един pass fix, не trial-and-error
- ✅ ListDir за探索, не читай рекурсивно всички файлове

---

## 📋 РАБОТЕН ЛОГ — Фаза 5 (Критични Fixes)

| # | Стъпка | Файлове | Приоритет | Статус |
|---|--------|---------|-----------|--------|
| 82 | Security: BuildConfig API Keys | `secrets.properties`, `app/build.gradle.kts`, `di/NetworkModule.kt` | 🔴 | ✅ |
| 83 | Room Migration 2→3 + 3→4 | `data/db/CryptoDatabase.kt`, `di/DatabaseModule.kt` | 🔴 | ✅ |
| 84 | CRTOverlay Performance Fix | `ui/components/crt/CRTOverlay.kt` | 🔴 | ✅ |
| 85 | Theme Unification Pass | всички UI файлове с hardcoded цветове | 🔴 | ✅ |
| 86 | CommandBar v2 (всички routes) | `ui/components/TerminalCommandBar.kt` | 🟡 | ✅ |
| 87 | AlertNotification Refactor | `service/AlertNotificationService.kt` | 🟡 | ✅ |
| 88 | ProGuard Rules Completion | `app/proguard-rules.pro` | 🟡 | ✅ |
| 89 | Room Index Optimization | `data/db/*.kt` | 🟡 | ✅ |
| 90 | Error Handling Global Strategy | `util/Result.kt`, всички Repositories | 🟡 | ✅ |

## 📋 РАБОТЕН ЛОГ — Фаза 6 (Performance & Architecture)

| # | Стъпка | Файлове | Приоритет | Статус |
|---|--------|---------|-----------|--------|
| 91 | FFT замяна на DFT в FourierPredictor | `domain/usecase/prediction/FourierCyclePredictor.kt` | 🔴 | ✅ |
| 92 | Prediction Cache Layer | `domain/usecase/prediction/PredictionCache.kt` | 🔴 | ✅ |
| 93 | Prediction Accuracy Tracker | `data/db/PredictionAccuracyEntity.kt`, `domain/usecase/PredictionAccuracyTracker.kt` | 🔴 | ✅ |
| 94 | SocketLifecycleManager Audit | `data/repository/CryptoRepositoryImpl.kt`, `service/SocketLifecycleManager.kt` | 🔴 | ✅ |
| 95 | Network OkHttp Cache | `di/NetworkModule.kt` | 🟡 | ✅ |
| 96 | ViewModelScope Audit | всички ViewModels | 🟡 | ✅ |

## 📋 РАБОТЕН ЛОГ — Фаза 7 (Завършване на екрани)

| # | Стъпка | Файлове | Приоритет | Статус |
|---|--------|---------|-----------|--------|
| 97 | Onboarding Sequence (3 screens) | `ui/onboarding/` | 🔴 | ✅ |
| 98 | EntryAnalyzerScreen v2 (full UI) | `ui/tools/EntryAnalyzerScreen.kt`, `viewmodel/EntryAnalyzerViewModel.kt` | 🔴 | ✅ |
| 99 | MTFScreen v2 (dynamic coins) | `ui/tools/MTFScreen.kt` | 🔴 | ✅ |
| 100 | BootSequenceScreen | `ui/splash/BootSequenceScreen.kt` | 🟡 | ✅ |
| 101 | PortfolioScreen (нов екран) | `ui/portfolio/` | 🔴 | ✅ |
| 102 | AnalysisScreen Completion | `ui/analysis/AnalysisScreen.kt` | 🔴 | ✅ |
| 103 | NewsScreen Real Implementation | `ui/news/NewsScreen.kt` | 🔴 | ✅ |
| 104 | Bloomberg Wall Screensaver | `ui/screensaver/BloombergWallScreen.kt` | 🟡 | ✅ |

## 📋 РАБОТЕН ЛОГ — Фаза 8 (AI & Иновации)

| # | Стъпка | Файлове | Приоритет | Статус |
|---|--------|---------|-----------|--------|
| 105 | Gemini AI Trade Coach | `data/api/GeminiApiService.kt`, `ui/ai/AICoachScreen.kt`, `viewmodel/AICoachViewModel.kt` | 🔴 | ✅ |
| 106 | Social Sentiment Feed | `data/api/SentimentApiService.kt`, `domain/usecase/SentimentAnalyzer.kt` | 🟡 | ✅ |
| 107a| Backtester Engine (Logic) | `domain/usecase/BacktesterEngine.kt` | 🟡 | ✅ |
| 107b| Backtester Engine (UI) | `ui/tools/BacktesterScreen.kt` | 🟡 | ✅ |
| 108a| Android Home Widget (Setup/Logic) | `widget/CryptoDeptWidgetReceiver.kt`, `res/xml/` | 🟡 | ✅ |
| 108b| Android Home Widget (Glance UI) | `widget/CryptoDeptWidget.kt` | 🟡 | ✅ |

## 📋 РАБОТЕН ЛОГ — Фаза 9 (Polish & UX)

| # | Стъпка | Файлове | Приоритет | Статус |
|---|--------|---------|-----------|--------|
| 109 | Haptic Feedback System | `util/HapticManager.kt` | 🟡 | ✅ |
| 110 | Matrix Rain Screensaver | `ui/screensaver/MatrixRainScreen.kt` | 🟡 | ✅ |
| 111 | Accessibility Pass | всички Screens | 🟡 | ✅ |
| 112 | Loading Skeleton v2 (per-screen) | `ui/components/skeletons/` | 🟡 | ✅ |

## 📋 РАБОТЕН ЛОГ — Фаза 10 (Production)

| # | Стъпка | Файлове | Приоритет | Статус |
|---|--------|---------|-----------|--------|
| 112 | Loading Skeleton v2 (per-screen) | `ui/components/skeletons/` | 🟡 | ✅ |
| 113 | Subscription System (Play Billing v6) | `data/billing/`, `ui/paywall/` | 🔴 | ✅ |
| 114 | Firebase Analytics Events | `util/AnalyticsManager.kt` | 🟡 | ✅ |
| 115 | API Rate Limit Handler | `data/api/RateLimitInterceptor.kt` | 🔴 | ✅ |
| 116a| UI Tests (Part A - Basic Screens) | `src/androidTest/ui/` | 🟡 | ✅ |
| 116b| UI Tests (Part B - Complex Flows) | `src/androidTest/ui/` | 🟡 | ✅ |
| 117 | App Review Request Logic | `util/ReviewManager.kt` | 🟡 | ✅ |

## 📋 РАБОТЕН ЛОГ — СЛОЙ I (Фондация и Стабилност)

| # | Стъпка | Файлове | Приоритет | Статус |
|---|--------|---------|-----------|--------|
| 120 | Quant/Backtest Crash Fix | `ui/tools/BacktesterScreen.kt`, `domain/usecase/BacktesterEngine.kt`, `viewmodel/BacktesterViewModel.kt`, `viewmodel/PositionSizeViewModel.kt`, `app/proguard-rules.pro` | 🔴 | ✅ |
| 121 | ProGuard Completion | `app/proguard-rules.pro` | 🔴 | ✅ |
| 122 | SoundManager Volume Fix | `service/SoundManager.kt`, `MainActivity.kt`, `AndroidManifest.xml` | 🔴 | ✅ |
| 123 | Data Extraction Rules + Backup | `res/xml/data_extraction_rules.xml`, `res/xml/backup_rules.xml` | 🟡 | ✅ |
| 151 | Production Hardening | `MainActivity.kt`, `util/ReviewManager.kt`, `CryptoDeptApplication.kt` | 🔴 | ✅ |
| 153 | BIG BOSS MODE (Admin Access) | `ui/paywall/PaywallScreen.kt`, `viewmodel/BillingViewModel.kt` | 🔴 | ✅ |

## 📋 РАБОТЕН ЛОГ — СЛОЙ II (Data & Intel)

| # | Стъпка | Файлове | Приоритет | Статус |
|---|--------|---------|-----------|--------|
 | 126 | Sentiment Integration | `domain/usecase/SentimentAnalyzer.kt`, `ui/analysis/AnalysisScreen.kt`, `ui/markets/MarketsScreen.kt`, `data/api/RssNewsParser.kt` | 🔴 | ✅ |
 | 132 | Correlation Matrix | `domain/usecase/CorrelationEngine.kt`, `ui/correlation/CorrelationScreen.kt` | 🔴 | ⏳ |
| 135 | Halving Cycle Analyzer | `domain/usecase/HalvingCycleAnalyzer.kt`, `ui/analysis/SeasonalScreen.kt` | 🔴 | ⏳ |
| 136 | DeFi Protocol Monitor | `data/api/DefiLlamaApi.kt`, `ui/defi/DeFiScreen.kt` | 🟡 | ⏳ |
| 137 | Macro Correlation Dashboard | `domain/usecase/MacroCorrelationEngine.kt`, `ui/macro/MacroCorrelationScreen.kt` | 🟡 | ⏳ |
| 138 | Advanced Signal Composer | `domain/usecase/SignalComposer.kt`, `ui/signals/SignalComposerScreen.kt` | 🔴 | ⏳ |
| 127 | Custom Composite Alerts | `domain/model/Alert.kt`, `ui/alerts/AlertsScreen.kt`, `service/AlertWorker.kt` | 🟡 | ⏳ |

## 📋 РАБОТЕН ЛОГ — СЛОЙ III (Visuals)

| # | Стъпка | Файлове | Приоритет | Статус |
|---|--------|---------|-----------|--------|
| 125 | Adaptive Icon Optimization | `res/mipmap-anydpi-v26/ic_launcher.xml`, `res/drawable/` | 🟡 | ⏳ |
| 128 | Coin Comparison Mode | `ui/compare/CompareScreen.kt`, `viewmodel/CompareViewModel.kt` | 🟡 | ⏳ |
| 129 | Share Prediction Card | `util/ShareManager.kt`, `ui/prediction/DeepAnalysisResultScreen.kt` | 🟡 | ⏳ |
| 140 | Heatmap Screensaver | `ui/screensaver/HeatmapScreen.kt` | 🟡 | ⏳ |

## 📋 РАБОТЕН ЛОГ — СЛОЙ IV (UX Domination)

| # | Стъпка | Файлове | Приоритет | Статус |
|---|--------|---------|-----------|--------|
| 124 | App Shortcuts | `res/xml/shortcuts.xml`, `MainActivity.kt` | 🟡 | ⏳ |
| 139 | AI Daily Briefing Push | `service/AIBriefingWorker.kt` | 🟡 | ⏳ |
| 141 | First Run Experience | `ui/onboarding/OnboardingScreen.kt` | 🔴 | ⏳ |
| 142 | “WHAT SHOULD I DO NOW?” Button | `ui/components/AdviceButton.kt` | 🔴 | ⏳ |
| 152 | Terminal Boot Sequence UI | `ui/splash/BootSequenceScreen.kt` | 🟡 | ⏳ |
| 133 | Tax Report Export (CSV) | `util/CsvExporter.kt`, `ui/journal/TradeJournalScreen.kt` | 🟡 | ⏳ |
| 143 | Personal Performance Tracker | `ui/portfolio/PerformanceStats.kt` | 🟡 | ⏳ |
| 144 | Trust Layer (Accuracy Stats) | `ui/components/AccuracyBadge.kt` | 🟡 | ⏳ |
| 145 | Glitch effects & Haptic feedback | `ui/components/Effects.kt` | 🟡 | ⏳ |
| 146 | “WHY THIS?” Explain Button | `ui/components/ExplainDialog.kt` | 🟡 | ⏳ |
| 147 | Smart Push Notifications | `service/NotificationManager.kt` | 🟡 | ⏳ |
| 148 | Offline Mode (Cache) | `data/repository/OfflineManager.kt` | 🟡 | ⏳ |
| 149 | Architecture & Memory Audit | всички ViewModels, Sockets | 🔴 | ⏳ |
| 150 | UI/UX Terminal Unification | всички UI файлове | 🔴 | ⏳ |

