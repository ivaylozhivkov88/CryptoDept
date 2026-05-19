package com.cryptodept.data.api

import com.cryptodept.BuildConfig
import com.cryptodept.domain.model.TradeJournal
import com.cryptodept.domain.repository.AIProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
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

        private fun getActiveProvider(): AIProvider {
            val hasValidKey = BuildConfig.GEMINI_API_KEY.isNotBlank() && !BuildConfig.GEMINI_API_KEY.contains("your_")
            return if (useBackendProxy || !hasValidKey) proxyProvider else geminiProvider
        }

        override suspend fun sendMessage(prompt: String): Flow<String> = 
            getActiveProvider().sendMessage(prompt).catch { e ->
                if (e.message?.contains("API key", ignoreCase = true) == true) {
                    emitAll(proxyProvider.sendMessage(prompt))
                } else {
                    throw e
                }
            }

        override suspend fun analyzeJournal(
            trades: List<TradeJournal>,
            currentRiskScore: Int,
            currentMarket: String,
        ): Flow<String> = 
            getActiveProvider().analyzeJournal(trades, currentRiskScore, currentMarket).catch { e ->
                if (e.message?.contains("API key", ignoreCase = true) == true) {
                    emitAll(proxyProvider.analyzeJournal(trades, currentRiskScore, currentMarket))
                } else {
                    throw e
                }
            }
    }
