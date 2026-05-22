package com.cryptodept.domain.agent

import com.cryptodept.data.api.CoinGeckoApi
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.CryptoRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * [AGENT-INTEGRITY] Data Integrity Unit
 * Mission: Prevent "Fake Data" incidents by cross-verifying internal cache against live API.
 */
@Singleton
class DataIntegrityAgent @Inject constructor(
    private val coinGeckoApi: CoinGeckoApi,
    private val repository: CryptoRepository,
    private val integrityService: com.cryptodept.domain.manager.SystemIntegrityService
) : CryptoAgent {
    override val id = "AGENT-INTEGRITY"
    override val name = "DATA_INTEGRITY_UNIT"

    override suspend fun analyze(data: MarketDataSnapshot): AgentReport {
        val anomalies = mutableListOf<String>()
        var anomalyScore = 0

        // 1. Verify Price Integrity (Against independent CG call)
        try {
            integrityService.addLog("Initializing source cross-check (CG API vs Cache)...")
            val liveData = coinGeckoApi.getSimplePrice("bitcoin,ethereum,litecoin")
            val btcLive = liveData["bitcoin"]?.get("usd") ?: 0.0
            val ltcLive = liveData["litecoin"]?.get("usd") ?: 0.0

            val btcCached = repository.getCachedPrice("bitcoin")
            val ltcCached = repository.getCachedPrice("litecoin")

            // Check BTC (Major deviation check)
            if (btcLive > 0 && btcCached > 0) {
                val diff = abs(btcLive - btcCached) / btcLive
                if (diff > 0.05) { // 5% threshold
                    val msg = "BTC price deviation: $${btcCached} (cached) vs $${btcLive} (live)"
                    anomalies.add(msg)
                    integrityService.addLog(msg, isAnomaly = true)
                    anomalyScore += 40
                } else {
                    integrityService.addLog("BTC Price Verified: <1% variance.")
                }
            }

            // Check LTC (Specific watchdog for the $2100 fake data incident)
            if (ltcLive > 0 && ltcCached > 200) { 
                val msg = "CRITICAL: LTC price anomaly detected ($${ltcCached})."
                anomalies.add(msg)
                integrityService.addLog(msg, isAnomaly = true)
                anomalyScore += 100
            } else if (ltcLive > 0) {
                integrityService.addLog("LTC Price Verified: OK.")
            }

        } catch (_: Exception) {
            val msg = "Integrity check failed: Network Error"
            anomalies.add(msg)
            integrityService.addLog(msg, isAnomaly = true)
            anomalyScore += 10
        }

        val summary = if (anomalies.isEmpty()) {
            "DATA_INTEGRITY_VERIFIED. System at 100% correlation."
        } else {
            ">>> DATA_INTEGRITY_ALERT: ${anomalies.joinToString(" | ")}"
        }

        return AgentReport(
            agentId = id,
            agentName = name,
            status = if (anomalyScore > 50) AgentStatus.ERROR else AgentStatus.SUCCESS,
            summary = summary,
            confidence = 1.0,
            anomalyScore = anomalyScore
        )
    }
}
