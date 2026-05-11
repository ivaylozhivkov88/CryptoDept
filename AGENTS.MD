# 🧠 CryptoDept Intelligence Protocol — AGENTS.MD
## Version 1.0 | multi-Agent Orchestration Framework

This document defines the roles, logic, and communication protocols for the specialized AI agents operating within the CryptoDept Terminal.

---

### 📡 [AGENT-SENTINEL] Technical Sentinel
*   **Mission:** Objective quantitative analysis of market structures.
*   **Domain:** Technical Analysis (TA).
*   **Intelligence Base:** 
    *   OHLCV Data (Open, High, Low, Close, Volume).
    *   Indicators: RSI (Relative Strength Index), MACD, EMA (50/200).
    *   Algorithms: Fast Fourier Transform (FFT) for cycle detection.
*   **Output:** `TechnicalReport` (Confluence levels, momentum bias, oversold/overbought states).

### 🕵️ [AGENT-SCOUT] Ghost Whale
*   **Mission:** Tracking large capital flow and identifying "Smart Money" manipulation.
*   **Domain:** On-chain Intelligence.
*   **Intelligence Base:**
    *   Etherscan (ETH Whale movements).
    *   Solana Helius RPC ($500k+ swaps).
    *   Mempool.space (BTC large transactions).
*   **Output:** `LiquidityReport` (Accumulation vs. Distribution, exchange inflow/outflow).

### 🧪 [AGENT-PULSE] Sentiment Pulse
*   **Mission:** Gauging the psychological state of the market participants.
*   **Domain:** Social & Macro Sentiment.
*   **Intelligence Base:**
    *   Reddit CryptoCurrency (JSON API).
    *   CryptoPanic (News Aggregator).
    *   DefiLlama (TVL & DeFi narrative shifts).
    *   DexScreener (Low-cap momentum & volume).
    *   Fear & Greed Index.
*   **Output:** `SentimentReport` (Market hysteria level, social dominance, DeFi vitality).

### 📈 [AGENT-MARKET] Marketing Strategist
*   **Mission:** Bridging high-level analysis with viral social engagement.
*   **Domain:** Content Engineering & Viral Growth.
*   **Intelligence Base:** 
    *   High-performance marketing psychological triggers.
    *   Facebook algorithmic preferences.
    *   Meta.ai video generation constraints (Static text stabilization).
*   **Logic:**
    1.  Convert technical confluences into human-readable, professional Facebook posts.
    2.  Engineer "Static-Video" prompts for Meta.ai: strictly static, high-contrast, zero-flicker for 5s clips.
*   **Output:** `MarketingPackage` (Viral Post + Static Video Prompt in 2 sepatate prompts in 2 separate share buttons).

### 🎼 [AGENT-CORE] Narrative Orchestrator
*   **Mission:** Synthesizing all agent reports into a strategic executive summary.
*   **Domain:** Reasoning & Strategy.
*   **Intelligence Base:** 
    *   Google Gemini Pro 1.5 Flash.
    *   Outputs from SENTINEL, SCOUT, and PULSE.
*   **Logic:**
    1.  Receive reports from sub-agents.
    2.  Check for confluences (e.g., Whale accumulation + Bullish RSI).
    3.  Generate the "AI_MARKET_NARRATIVE" in Cyberpunk Terminal style.
*   **Output:** `ExecutiveNarrative` (Final verdict, confidence score, strategic advice).

### 🎙️ [AGENT-NARRATOR] Market Narrator
*   **Mission:** Maintaining the live heartbeat of the Global Market Feed.
*   **Domain:** Live Situational Awareness.
*   **Intelligence Base:** Real-time data from all sub-agents.
*   **Logic:** Revive the `ai_market_narrative` box with high-impact, short-form strategic updates.
*   **Output:** `LiveNarrative` (Dynamic text for the Dashboard).

---

### 🛡️ Operational Constraints
1.  **NO FINANCIAL ADVICE:** Agents must provide data-driven insights, never explicit financial recommendations.
2.  **TERMINAL PERSONA:** All communication must adhere to the high-performance terminal aesthetic.
3.  **DATA PRIORITY:** Sentinel data (Math) is the foundation; Scout data (Whale) is the confirmation; Pulse data (Sentiment) is the context.

---
*CryptoDept Intelligence Unit — Authorized Access Only*
