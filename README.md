# 📟 CryptoDept Terminal — Elite Market Intelligence Suite

[![Platform](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-blue?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean_/_MVVM-orange?style=for-the-badge)](https://developer.android.com/topic/architecture)

**CryptoDept Terminal** is a high-performance, production-ready cryptocurrency intelligence tool designed for professional traders. It moves away from standard "app-like" UIs into a sophisticated **Cyberpunk Terminal Aesthetic**, prioritizing data throughput, real-time analytics, and AI-driven insights.

---

## 🚀 Key Features (Engineering Excellence)

### 🤖 Ensemble AI Market Narrative
Powered by **Google Gemini AI**, the terminal synthesizes raw technical data (RSI, MACD, Volume) and live sentiment from Reddit/CryptoPanic into a coherent, strategic report. 
*   *Implementation:* Custom AI prompt engineering and state-driven report generation.

### 🐳 Real-time Multi-Chain Whale Tracker
Monitors the blockchain for massive liquidity moves (> $500k) across **Bitcoin, Ethereum, and Solana**.
*   *Implementation:* Integrated Etherscan, Mempool.space, and Helius RPC APIs with aggressive caching and rate-limit handling.

### 📊 Multi-Timeframe Analysis Engine
Proprietary logic that evaluates market bias across 1H, 4H, and Daily timeframes to find confluence.
*   *Implementation:* High-performance math engine using Kotlin Flow for reactive updates.

### 📺 Dynamic State-Driven Screensavers
Custom-built **Matrix Rain (with Katakana characters)** and **Heatmap** screensavers that display live market prices using Compose Canvas.
*   *Implementation:* Optimized rendering pipeline at **60 FPS** with zero main-thread blockage.

---

## 🏗 Architecture & Technical Stack

The project is built on **Clean Architecture** principles, ensuring modularity, testability, and massive scalability.

| Layer | Technology |
| :--- | :--- |
| **UI Layer** | 100% **Jetpack Compose** with custom `TerminalTheme` & Material 3. |
| **Dependency Injection** | **Hilt** (Dagger) for decoupled component management. |
| **Asynchronous Logic** | **Kotlin Coroutines & Flow** (SharedFlow/StateFlow) for reactive data streams. |
| **Networking** | **Retrofit + OkHttp** with custom Interceptors and SSL Pinning. |
| **Persistence** | **Room DB** for offline-first caching and **DataStore** for user preferences. |
| **Background Tasks** | **WorkManager** for scheduled news sync and market alerts. |

---

## 🔒 Security & Performance

*   **Firebase Authentication:** Secure Google Sign-In integration for operator-level access.
*   **Encrypted Storage:** Sensitive API keys and user data are protected via **SQLCipher** and **EncryptedSharedPreferences**.
*   **Obfuscation:** Production-ready **R8/ProGuard** rules for code shrinking and security hardening.
*   **Stability:** Full Firebase suite integration (**Crashlytics, Analytics, Performance Monitoring**) for real-time health tracking.

---

## 🛠 Setup & Requirements

1.  **JDK 17+**
2.  **Android SDK 35**
3.  **Local Keys:** Requires a `local.properties` file with API keys for Gemini, CoinGecko, and Firebase. See `local.properties.example` for details.

---

## 👨‍💻 Developer Setup

### Static Analysis
This project uses **Detekt** and **Ktlint** to maintain code quality.
*   **Detekt:** Run `./gradlew detekt` to check for code smells.
*   **Ktlint:** Run `./gradlew ktlintCheck` or `./gradlew ktlintFormat` to fix formatting.

### Masterplan
See [CRYPTODEPT_MASTERPLAN_v4.md](CRYPTODEPT_MASTERPLAN_v4.md) for the detailed development roadmap.

---
*Developed by CryptoDept Engineering Team*
