package com.cryptodept.domain.agent

import com.cryptodept.domain.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentGovernanceTest {

    @Test
    fun `verify sentinel identity`() {
        val agent = TechnicalSentinel()
        assertEquals("AGENT-SENTINEL", agent.id)
        assertEquals("TECHNICAL_SENTINEL", agent.name)
    }

    @Test
    fun `verify agent report formatting`() = runBlocking {
        val agent = WhaleScout()
        val dummyData = MarketDataSnapshot(
            price = 70000.0,
            rsi = 50.0,
            macdSignal = "N/A",
            ema50Signal = "N/A",
            ema200Signal = "N/A",
            bollingerPosition = "N/A",
            fundingRate = 0.0,
            fundingLevel = "N/A",
            longLiquidations24h = 0.0,
            shortLiquidations24h = 0.0,
            fearGreedIndex = 50,
            newsSentiment = "NEUTRAL",
            wyckoffPhase = "N/A",
            elliottWave = "N/A",
            riskScore = 50,
            priceChange24h = 0.0,
            btcDominance = 50.0,
            sp500Change = 0.0,
            dxyChange = 0.0
        )
        
        val report = agent.analyze(dummyData)
        assertEquals("AGENT-SCOUT", report.agentId)
        assertEquals("GHOST_WHALE", report.agentName)
        assertEquals(AgentStatus.READY, report.status)
    }
}
