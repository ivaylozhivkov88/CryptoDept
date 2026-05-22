# 🏗 Architectural Guide: Multi-Agent Orchestration

This document explains the technical implementation of the AI layer within **CryptoDept**.

## 1. The Multi-Agent Coordinator
Unlike monolithic AI apps, CryptoDept uses a **Coordinator Pattern**. 
Location: `com.cryptodept.domain.usecase.MultiAgentCoordinator`

The Coordinator accepts a `MarketDataSnapshot` and delegates analysis to a list of `CryptoAgent` implementations.

## 2. Communication Protocol
Agents communicate via a unified `AgentReport` object:
```kotlin
data class AgentReport(
    val agentId: String,
    val summary: String,
    val anomalyScore: Int,
    val status: AgentStatus
)
```

## 3. Adding a New Agent
To add a new intelligence node:
1. Implement the `CryptoAgent` interface.
2. Define the agent's logic in the `analyze()` method.
3. Register the agent in `AgentModule.kt` (Hilt).
4. Update `AGENTS.MD` to include the new node's mission.

## 4. UI/UX: The Phosphor Theme
The terminal aesthetic is achieved through a custom Compose Theme located in `com.cryptodept.ui.theme`. 
*   **Grid System:** Uses 8dp spacing for high-density layouts.
*   **Scan Lines:** A global overlay composable found in `com.cryptodept.ui.components.ScanLineOverlay`.

---
*CryptoDept Intelligence Unit*
