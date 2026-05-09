package com.cryptodept.data.api

import com.cryptodept.domain.model.TradeJournal
import com.cryptodept.domain.repository.AIProvider
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIProviderRouter
    @Inject
    constructor(
        private val geminiProvider: GeminiCoachService,
        private val proxyProvider: ProxyAIProvider,
    ) : AIProvider {
        // In a real scenario, this would come from DataStore or RemoteConfig
        private var useBackendProxy: Boolean = false

        fun setUseBackendProxy(enabled: Boolean) {
            useBackendProxy = enabled
        }

        private fun getActiveProvider(): AIProvider = if (useBackendProxy) proxyProvider else geminiProvider

        override suspend fun sendMessage(prompt: String): Flow<String> = getActiveProvider().sendMessage(prompt)

        override suspend fun analyzeJournal(
            trades: List<TradeJournal>,
            currentRiskScore: Int,
            currentMarket: String,
        ): Flow<String> = getActiveProvider().analyzeJournal(trades, currentRiskScore, currentMarket)
    }
