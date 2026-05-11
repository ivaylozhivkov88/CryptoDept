package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiAgentCoordinator @Inject constructor(
    private val sentinel: TechnicalSentinel,
    private val scout: WhaleScout,
    private val pulse: SentimentPulse,
    private val orchestrator: NarrativeOrchestrator
) {
    suspend fun runOrchestration(snapshot: MarketDataSnapshot): AgentReport = coroutineScope {
        val sentinelJob = async { sentinel.analyze(snapshot) }
        val scoutJob = async { scout.analyze(snapshot) }
        val pulseJob = async { pulse.analyze(snapshot) }

        val reports = listOf(sentinelJob.await(), scoutJob.await(), pulseJob.await())
        
        // Final synthesis by the Orchestrator
        orchestrator.analyze(snapshot) 
    }
}
