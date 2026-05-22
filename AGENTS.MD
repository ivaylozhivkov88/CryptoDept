# 🧠 CryptoDept Intelligence Protocol — AGENTS.MD
## Version 1.1 | multi-Agent Orchestration Framework

This document defines the roles, logic, and communication protocols for the specialized AI agents operating within the CryptoDept Terminal.

---

### 📡 [AGENT-SENTINEL] Technical Sentinel
*   **Mission:** Objective quantitative analysis of market structures.
*   **Domain:** Technical Analysis (TA).
*   **Intelligence Base:** OHLCV Data, RSI, MACD, EMA (50/200), FFT.
*   **Output:** `TechnicalReport` (Confluence levels, momentum bias).

### 🕵️ [AGENT-SCOUT] Ghost Whale
*   **Mission:** Tracking large capital flow and identifying "Smart Money" manipulation.
*   **Domain:** On-chain Intelligence.
*   **Intelligence Base:** Etherscan, Helius RPC ($500k+ swaps), Mempool.space.
*   **Output:** `LiquidityReport` (Accumulation vs. Distribution).

### 🧪 [AGENT-PULSE] Sentiment Pulse
*   **Mission:** Gauging the psychological state of market participants.
*   **Domain:** Social & Macro Sentiment.
*   **Intelligence Base:** Reddit, CryptoPanic, Fear & Greed Index.
*   **Output:** `SentimentReport` (Social dominance, market hysteria level).

### 🔮 [AGENT-QUANT] The Oracle
*   **Mission:** Strategic price forecasting using ensemble mathematical modeling.
*   **Domain:** Predictive Analytics.
*   **Intelligence Base:** Ensemble of 7 models (Fourier, Monte Carlo, Wyckoff, Elliott, Hurst, Linear Regression).
*   **Output:** `OraclePrediction` (Multi-timeframe price targets, probability distribution).

### 📈 [AGENT-MARKET] Marketing Strategist
*   **Mission:** Bridging high-level analysis with viral social engagement.
*   **Domain:** Content Engineering & Viral Growth.
*   **Logic:** Converts technical confluences into professional social media reports and AI video prompts.
*   **Output:** `MarketingPackage` (Viral Post + Video Prompt).

### ⚖️ [AGENT-AUDITOR] Fiscal Treasury
*   **Mission:** Ensuring financial integrity and subscription service uptime.
*   **Domain:** Revenue & Billing Integrity.
*   **Intelligence Base:** Google Play Billing Service, Secure Preferences.
*   **Output:** `FiscalReport` (Subscription verification, pass expiry tracking).
*   **Status:** [BETA] Basic logic integrated into `BillingService.kt`. Server-side validation planned for v1.5.

### 🛡️ [AGENT-SYSTRACE] System Auditor
*   **Mission:** Real-time monitoring of app stability and error mitigation.
*   **Domain:** System Health & Stability.
*   **Intelligence Base:** Firebase Crashlytics, Performance Monitoring.
*   **Output:** `StabilityReport` (Error logs, crash prevention metrics).

### 👮 [AGENT-INTEGRITY] Data Verification Unit
*   **Mission:** Preventing "Fake Data" incidents and verifying source stream accuracy.
*   **Domain:** Data Integrity & Cross-Verification.
*   **Intelligence Base:** Independent API checks (CoinGecko/Kraken/Binance).
*   **Output:** `IntegrityReport` (Price variance analysis, cache health).
*   **Status:** [ACTIVE] Integrated into `MultiAgentCoordinator`.

### ✍️ [AGENT-SCRIBE] Documentation Architect
*   **Mission:** Converting complex multi-agent logic and Android architecture into world-class documentation.
*   **Domain:** Technical Writing, Legal Compliance, & Onboarding UX.
*   **Intelligence Base:** Project codebase, Android SDK standards, Regulatory requirements.
*   **Output:** `README.md`, `PRIVACY_POLICY.md`, `ARCHITECTURAL_GUIDE.md`.

### 🎼 [AGENT-CORE] Narrative Orchestrator
*   **Mission:** Synthesizing all sub-agent reports into a strategic executive summary.
*   **Domain:** Reasoning & Strategy.
*   **Intelligence Base:** Outputs from all sub-agents.
*   **Output:** `ExecutiveNarrative` (Final verdict, confidence score, strategic advice).

### 🎙️ [AGENT-NARRATOR] Market Narrator
*   **Mission:** Maintaining the live heartbeat of the Global Market Feed.
*   **Domain:** Situational Awareness.
*   **Intelligence Base:** Real-time data from all sub-agents.
*   **Output:** `LiveNarrative` (Dynamic text for the Dashboard).

---

### 🛡️ Operational Constraints
1.  **NO FINANCIAL ADVICE:** Agents provide data-driven insights only.
2.  **DATA PRIORITY:** Sentinel (Math) > Scout (Whale) > Pulse (Sentiment).
3.  **STABILITY PRIORITY:** SYSTRACE logs take precedence over analysis during critical system failures.

---
*CryptoDept Intelligence Unit — Authorized Access Only*
