# 📟 CryptoDept Terminal v1.1
> **The Multi-Agent Intelligence Hub for Digital Assets.**

![Android](https://img.shields.io/badge/Platform-Android-00FF41?style=for-the-badge&logo=android&logoColor=black)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-00FF41?style=for-the-badge&logo=kotlin&logoColor=black)
![Compose](https://img.shields.io/badge/UI-Jetpack_Compose-00FF41?style=for-the-badge&logo=jetpackcompose&logoColor=black)
![License](https://img.shields.io/badge/License-Proprietary-red?style=for-the-badge)

**CryptoDept** is not just another tracker. It is a high-performance Android terminal designed for professional traders, powered by a decentralized-style **Multi-Agent Orchestration Framework**. It synthesizes technical analysis, on-chain whale movement, and social sentiment into a single, cohesive market narrative.

---

## 🚀 Core Features

### 🧠 Multi-Agent Intelligence
The heart of the terminal consists of specialized AI agents:
*   **[SENTINEL]**: Quantitative Technical Analysis (RSI, MACD, FFT).
*   **[GHOST WHALE]**: Tracking capital flow via Etherscan & Helius.
*   **[PULSE]**: Real-time social sentiment analysis.
*   **[ORACLE]**: Predictive mathematical modeling (Fourier, Monte Carlo).

### ⚡ Professional Trading Tools
*   **Liquidation Heatmaps**: Visualize where the "pain" is for market participants.
*   **Whale Feed**: Real-time alerts for large $500k+ swaps.
*   **Global Market Dashboard**: 3D Gauges for Fear & Greed and Altcoin Season.
*   **Terminal-Style UX**: Designed for high-density information display.

---

## 🛠 Tech Stack

*   **UI**: Jetpack Compose with custom Phosphor/Monospace theme.
*   **Architecture**: Clean Architecture + MVVM + MVI-lite (Flow-driven).
*   **Dependency Injection**: Hilt.
*   **Database**: Room (with encrypted storage for sensitive data).
*   **Networking**: Retrofit & OKHttp (Multi-source aggregation).
*   **Real-time**: WebSockets for live price consensus.

---

## 🔧 Installation & Setup

### Requirements
*   **Android Studio**: Ladybug (2024.2.1) or newer.
*   **JDK**: 17+.
*   **API Level**: 26+ (Android 8.0).

### Local Configuration
To build the project, you must provide your own API keys. Create a `local.properties` file in the root directory:

```properties
# D:/CryptoDept/local.properties
COINGECKO_API_KEY=your_key_here
COINGLASS_API_KEY=your_key_here
CRYPTOPANIC_API_KEY=your_key_here
HELIUS_API_KEY=your_key_here
```

### Build Instructions
1. Clone the repository.
2. Sync Gradle.
3. Run `:app:assembleDebug`.

---

## 🏗 Architecture Overview

The project follows a strict **Layered Clean Architecture**:
1.  **`:domain`**: Pure Kotlin. Contains Use Cases, Repository Interfaces, and the `MultiAgentCoordinator`.
2.  **`:data`**: Implementations of repositories, Retrofit APIs, and Room DAOs.
3.  **`:ui`**: Jetpack Compose screens, ViewModels, and the Phosphor Theme engine.

---

## 📄 Compliance & Legal
*   [Privacy Policy](PRIVACY_POLICY.md)
*   [Delete Account Instructions](DELETE_ACCOUNT.md)
*   [Intelligence Protocols (AGENTS.MD)](AGENTS.MD)

---
*CryptoDept Intelligence Unit — Authorized Access Only*
