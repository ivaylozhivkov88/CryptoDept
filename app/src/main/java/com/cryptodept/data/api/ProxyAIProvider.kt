package com.cryptodept.data.api

import com.cryptodept.domain.model.TradeJournal
import com.cryptodept.domain.repository.AIProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProxyAIProvider
    @Inject
    constructor() : AIProvider {
        override suspend fun sendMessage(prompt: String): Flow<String> =
            flowOf("[PROXY] Backend relay active. Simulated response for: $prompt")

        override suspend fun analyzeJournal(
            trades: List<TradeJournal>,
            currentRiskScore: Int,
            currentMarket: String,
        ): Flow<String> = flowOf("[PROXY] Analyzing ${trades.size} trades via backend...")
    }
