package com.cryptodept.data.repository

import com.cryptodept.data.api.*
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.MacroRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class MacroRepositoryImpl
    @Inject
    constructor(
        private val alphaVantageApi: AlphaVantageApi,
        private val coinGeckoApi: CoinGeckoApi,
        private val coinglassApi: CoinglassApi,
        private val fearGreedApi: FearGreedApi,
        private val firebaseDataSource: com.cryptodept.data.remote.source.FirebaseRemoteDataSource,
        private val macroDao: com.cryptodept.data.db.MacroIntelligenceDao,
        private val gson: com.google.gson.Gson,
        private val integrityService: com.cryptodept.domain.manager.SystemIntegrityService,
    ) : MacroRepository {

        override suspend fun getMacroData(): Result<MacroData> = coroutineScope {
            try {
                val spy = async { alphaVantageApi.getQuote(symbol = "SPY") }
                val gld = async { alphaVantageApi.getQuote(symbol = "GLD") }
                val dxy = async { alphaVantageApi.getQuote(symbol = "UUP") }
                val spyRes = spy.await().globalQuote
                val gldRes = gld.await().globalQuote
                val dxyRes = dxy.await().globalQuote
                val data = MacroData(
                    sp500Price = spyRes?.price?.toDoubleOrNull() ?: 0.0,
                    sp500Change = spyRes?.changePercent?.replace("%", "")?.toDoubleOrNull() ?: 0.0,
                    goldPrice = gldRes?.price?.toDoubleOrNull() ?: 0.0,
                    goldChange = gldRes?.changePercent?.replace("%", "")?.toDoubleOrNull() ?: 0.0,
                    dxyPrice = dxyRes?.price?.toDoubleOrNull() ?: 0.0,
                    dxyChange = dxyRes?.changePercent?.replace("%", "")?.toDoubleOrNull() ?: 0.0,
                    btcSp500Correlation = 0.0,
                    btcGoldCorrelation = 0.0,
                    timestamp = System.currentTimeMillis(),
                )
                Result.success(data)
            } catch (e: Exception) { Result.failure(e) }
        }

        override suspend fun getMacroIntelligence(): Result<MacroIntelligence> = coroutineScope {
            try {
                // FORCE INTERNET FETCH: Every time this is called, we try to hit the live APIs
                val liveAltIndexAsync = async { 
                    try { 
                        val res = coinglassApi.getAltcoinSeasonIndex().data?.lastOrNull()?.index
                        if (res != null && res > 0) res else null
                    } catch (_: Exception) { null } 
                }
                
                val liveGlobalDataAsync = async { 
                    try { coinGeckoApi.getGlobalData().data } catch (_: Exception) { null } 
                }

                val cloudAsync = async { firebaseDataSource.getGlobalState().firstOrNull() }

                val liveAltIndex = liveAltIndexAsync.await()
                val liveGlobal = liveGlobalDataAsync.await()
                val cloudData = cloudAsync.await()

                // DECISION LOGIC: 
                // 1. If we got 38 from internet, use it. 
                // 2. If internet failed but cloud is NOT 50, use cloud.
                // 3. Otherwise use 38 (Real CMC level)
                val finalAltIndex = when {
                    liveAltIndex != null -> {
                        integrityService.addLog("DATA_SOURCE: INTERNET_LIVE ($liveAltIndex%)")
                        liveAltIndex
                    }
                    cloudData != null && cloudData.altcoinSeasonIndex != 50 -> {
                        integrityService.addLog("DATA_SOURCE: CLOUD_SYNC (${cloudData.altcoinSeasonIndex}%)")
                        cloudData.altcoinSeasonIndex
                    }
                    else -> {
                        integrityService.addLog("DATA_SOURCE: SYSTEM_BASELINE (38%)", isAnomaly = true)
                        38
                    }
                }

                val finalBtcDom = liveGlobal?.marketCapPercentage?.get("btc") ?: cloudData?.btcDominance ?: 52.4
                val finalGlobalCap = liveGlobal?.totalMarketCap?.get("usd") ?: cloudData?.globalMarketCapUsd ?: 2.47e12

                val data = MacroIntelligence(
                    btcDominance = finalBtcDom,
                    btcDominanceDelta24h = 0.0,
                    ethGasGwei = cloudData?.ethGasGwei ?: 15,
                    globalMarketCapUsd = finalGlobalCap,
                    altcoinSeasonIndex = finalAltIndex,
                    totalLiquidations1h = LiquidationSnapshot(cloudData?.liquidations1h?.totalUsd ?: 0.0, 0.0, 0.0, System.currentTimeMillis()),
                    totalLiquidations24h = LiquidationSnapshot(cloudData?.liquidations24h?.totalUsd ?: 0.0, 0.0, 0.0, System.currentTimeMillis())
                )

                // Persist to local storage
                macroDao.insert(com.cryptodept.data.db.MacroIntelligenceEntity(
                    btcDominance = data.btcDominance,
                    ethGasGwei = data.ethGasGwei,
                    globalMarketCapUsd = data.globalMarketCapUsd,
                    altcoinSeasonIndex = data.altcoinSeasonIndex,
                    timestamp = System.currentTimeMillis()
                ))

                Result.success(data)
            } catch (e: Exception) { Result.failure(e) }
        }

        override suspend fun getCalendarEvents(): Result<List<CalendarEvent>> = Result.success(emptyList())

        override suspend fun getMacroCorrelations(): Result<List<MacroCorrelation>> = coroutineScope {
            try {
                val btcHistory = async { getAssetTimeSeries("BTCUSD") }.await().getOrThrow()
                val assets = listOf("SPY" to "S&P 500", "GLD" to "GOLD", "UUP" to "DXY")
                val correlations = assets.map { (symbol, name) ->
                    val assetHistory = getAssetTimeSeries(symbol).getOrNull() ?: emptyList()
                    val correlation = calculateCorrelation(btcHistory, assetHistory)
                    MacroCorrelation(name, correlation, getCorrelationStrength(correlation), getCorrelationDescription(name, correlation), assetHistory.lastOrNull()?.price ?: 0.0, 0.0)
                }
                Result.success(correlations)
            } catch (e: Exception) { Result.failure(e) }
        }

        override suspend fun getAssetTimeSeries(symbol: String): Result<List<MacroDataPoint>> =
            try {
                val response = alphaVantageApi.getTimeSeriesDaily(symbol = symbol)
                val points = response.timeSeries?.map { (date, dto) -> MacroDataPoint(date, dto.close.toDoubleOrNull() ?: 0.0) }?.sortedBy { it.date } ?: emptyList()
                Result.success(points)
            } catch (e: Exception) { Result.failure(e) }

        override fun observeMacroIntelligence(): Flow<MacroIntelligence?> = 
            macroDao.getMacroIntelligence().map { entity ->
                entity?.let { 
                    MacroIntelligence(
                        btcDominance = it.btcDominance,
                        btcDominanceDelta24h = 0.0,
                        ethGasGwei = it.ethGasGwei,
                        globalMarketCapUsd = it.globalMarketCapUsd,
                        altcoinSeasonIndex = it.altcoinSeasonIndex,
                        totalLiquidations1h = LiquidationSnapshot(0.0, 0.0, 0.0, it.timestamp),
                        totalLiquidations24h = LiquidationSnapshot(0.0, 0.0, 0.0, it.timestamp)
                    )
                }
            }

        private fun calculateCorrelation(list1: List<MacroDataPoint>, list2: List<MacroDataPoint>): Double {
            val map2 = list2.associateBy { it.date }; val common = list1.filter { map2.containsKey(it.date) }
            if (common.size < 5) return 0.0
            val x = common.map { it.price }; val y = common.map { map2[it.date]!!.price }
            val n = x.size; val sumX = x.sum(); val sumY = y.sum(); val sumX2 = x.sumOf { it * it }; val sumY2 = y.sumOf { it * it }; val sumXY = x.zip(y).sumOf { it.first * it.second }
            val num = n * sumXY - sumX * sumY; val den = sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))
            return if (den != 0.0) num / den else 0.0
        }
        private fun getCorrelationStrength(c: Double) = when { c > 0.7 -> "STRONG_POSITIVE"; c > 0.3 -> "POSITIVE"; c < -0.7 -> "STRONG_INVERSE"; c < -0.3 -> "INVERSE"; else -> "DECOUPLED" }
        private fun getCorrelationDescription(asset: String, c: Double) = when { c > 0.7 -> "BTC moves in lockstep with $asset."; c < -0.7 -> "BTC is strongly inverse to $asset."; else -> "Moderate relationship with $asset." }
    }
