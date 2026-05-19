package com.cryptodept.domain.usecase

import com.cryptodept.domain.agent.DataIntegrityAgent
import com.cryptodept.domain.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MultiAgentCoordinator @Inject constructor(
    private val sentinel: TechnicalSentinel,
    private val pulse: SentimentPulse,
    private val fbi: OversightSentinel,
    private val integrity: DataIntegrityAgent,
    private val orchestrator: NarrativeOrchestrator
) {
    suspend fun runOrchestration(snapshot: MarketDataSnapshot): AgentReport = coroutineScope {
        withTimeoutOrNull(15000) {
            val sentinelJob = async { sentinel.analyze(snapshot) }
            val pulseJob = async { pulse.analyze(snapshot) }
            val fbiJob = async { fbi.analyze(snapshot) }
            val integrityJob = async { integrity.analyze(snapshot) }

            // We wait for all but if they take too long, withTimeoutOrNull returns null
            sentinelJob.await()
            pulseJob.await()
            fbiJob.await()
            integrityJob.await()
        }
        
        // Final synthesis by the Orchestrator with existing data
        orchestrator.analyze(snapshot) 
    }
}
