package com.cryptodept.data.repository

import com.cryptodept.data.api.BinanceFuturesApi
import com.cryptodept.data.api.CoinglassApi
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.DerivativesRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DerivativesRepositoryImpl
    @Inject
    constructor(
        private val binanceApi: BinanceFuturesApi,
        private val coinglassApi: CoinglassApi,
    ) : DerivativesRepository {
        private fun resolveBinanceSymbol(symbol: String): String {
            val base = when (symbol.uppercase()) {
                "BITCOIN" -> "BTC"
                "ETHEREUM" -> "ETH"
                "LITECOIN" -> "LTC"
                "RIPPLE" -> "XRP"
                "BINANCECOIN" -> "BNB"
                "SOLANA" -> "SOL"
                "CARDANO" -> "ADA"
                "DOGECOIN" -> "DOGE"
                "TRON" -> "TRX"
                "POLKADOT" -> "DOT"
                "CHAINLINK" -> "LINK"
                "AVALANCHE" -> "AVAX"
                "POLYGON" -> "MATIC"
                else -> symbol.uppercase()
            }
            return if (base.endsWith("USDT")) base else "${base}USDT"
        }

        override suspend fun getFundingRate(symbol: String): Result<FundingRateData> =
            try {
                val binanceSymbol = resolveBinanceSymbol(symbol)
                val binanceResponse = binanceApi.getFundingRate(binanceSymbol)
                val coinglassResponse =
                    try {
                        coinglassApi.getAggregatedFunding(symbol)
                    } catch (e: Exception) {
                        null
                    }

                val binanceRate = binanceResponse.lastFundingRate.toDouble() * 100 // Convert to %
                val avgRate = coinglassResponse?.data?.map { it.rate }?.average() ?: binanceRate

                val data =
                    FundingRateData(
                        symbol = symbol,
                        markPrice = binanceResponse.markPrice.toDouble(),
                        binanceRate = binanceRate,
                        aggregatedRate = avgRate,
                        nextFundingTime = binanceResponse.nextFundingTime,
                        rateLevel =
                            when {
                                avgRate > 0.05 -> FundingLevel.HIGH
                                avgRate < -0.02 -> FundingLevel.LOW
                                else -> FundingLevel.NORMAL
                            },
                        timestamp = System.currentTimeMillis(),
                    )
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun getOpenInterest(symbol: String): Result<OpenInterestData> =
            try {
                val binanceSymbol = resolveBinanceSymbol(symbol)
                val oi = binanceApi.getOpenInterest(binanceSymbol)
                val hist = binanceApi.getOpenInterestHistory(binanceSymbol)

                val currentOI = oi.openInterest.toDouble()
                val prevOI = hist.firstOrNull()?.sumOpenInterest?.toDouble() ?: currentOI
                val change = if (prevOI != 0.0) ((currentOI - prevOI) / prevOI) * 100 else 0.0

                val data =
                    OpenInterestData(
                        symbol = symbol,
                        openInterestUsd = currentOI,
                        openInterestChange24h = change,
                        trend = OITrend.RISING_WITH_PRICE, // Simplified
                        history = emptyList(),
                        timestamp = System.currentTimeMillis(),
                    )
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun getLiquidationData(symbol: String): Result<LiquidationData> =
            try {
                val heatmap = coinglassApi.getLiquidationHeatmap(symbol)
                val summary =
                    try {
                        coinglassApi.getGlobalLiquidations(symbol)
                    } catch (e: Exception) {
                        null
                    }

                val levels =
                    heatmap.data?.let { data ->
                        data.pricelevels.mapIndexed { index, price ->
                            val longVal = data.longLiquidations.getOrNull(index) ?: 0.0
                            val shortVal = data.shortLiquidations.getOrNull(index) ?: 0.0
                            LiquidationLevel(
                                price = price,
                                longLiquidationUsd = longVal,
                                shortLiquidationUsd = shortVal,
                                isSignificant = (longVal + shortVal) > 1_000_000, // Over 1M is significant for heatmap
                            )
                        }
                    } ?: emptyList()

                val data =
                    LiquidationData(
                        symbol = symbol,
                        longLiquidations24h = summary?.data?.longVolUsd ?: 0.0,
                        shortLiquidations24h = summary?.data?.shortVolUsd ?: 0.0,
                        dominantSide = if ((summary?.data?.longVolUsd ?: 0.0) > (summary?.data?.shortVolUsd ?: 0.0)) "LONGS" else "SHORTS",
                        heatmapLevels = levels,
                        timestamp = System.currentTimeMillis(),
                    )
                Result.success(data)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun getLongShortRatio(symbol: String): Result<Pair<Double, Double>> =
            try {
                val binanceSymbol = resolveBinanceSymbol(symbol)
                val ratio = binanceApi.getLongShortRatio(binanceSymbol).lastOrNull()
                val long = ratio?.longAccount?.toDouble() ?: 0.5
                val short = ratio?.shortAccount?.toDouble() ?: 0.5
                Result.success(long to short)
            } catch (e: Exception) {
                Result.failure(e)
            }

        override suspend fun getFundingHeatmap(): Result<List<FundingHeatmapItem>> =
            coroutineScope {
                try {
                    val topCoins = listOf("BTC", "ETH", "SOL", "BNB", "XRP", "ADA", "DOGE", "TRX", "LINK", "DOT")
                    val heatmap =
                        topCoins.map { symbol ->
                            async {
                                try {
                                    val response = coinglassApi.getAggregatedFunding(symbol)
                                    val binance = response.data.find { it.exchangeName == "Binance" }?.rate ?: 0.0
                                    val bybit = response.data.find { it.exchangeName == "Bybit" }?.rate ?: 0.0
                                    val okx = response.data.find { it.exchangeName == "OKX" }?.rate ?: 0.0
                                    val avg = response.data.map { it.rate }.average()
                                    FundingHeatmapItem(symbol, binance, bybit, okx, avg)
                                } catch (e: Exception) {
                                    null
                                }
                            }
                        }.awaitAll().filterNotNull()
                    Result.success(heatmap)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override fun getLiquidationHeatmap(symbol: String): Flow<LiquidationData> =
            flow {
                getLiquidationData(symbol).onSuccess { emit(it) }
            }
    }
