package com.cryptodept.data.repository

import com.cryptodept.data.api.AlphaVantageApi
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.MacroRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class MacroRepositoryImpl
    @Inject
    constructor(
        private val alphaVantageApi: AlphaVantageApi,
    ) : MacroRepository {
        override suspend fun getMacroData(): Result<MacroData> =
            coroutineScope {
                try {
                    val spy = async { alphaVantageApi.getQuote(symbol = "SPY") }
                    val gld = async { alphaVantageApi.getQuote(symbol = "GLD") }
                    val dxy = async { alphaVantageApi.getQuote(symbol = "UUP") }

                    val spyRes = spy.await().globalQuote
                    val gldRes = gld.await().globalQuote
                    val dxyRes = dxy.await().globalQuote

                    val data =
                        MacroData(
                            sp500Price = spyRes?.price?.toDoubleOrNull() ?: 0.0,
                            sp500Change = spyRes?.changePercent?.replace("%", "")?.toDoubleOrNull() ?: 0.0,
                            goldPrice = gldRes?.price?.toDoubleOrNull() ?: 0.0,
                            goldChange = gldRes?.changePercent?.replace("%", "")?.toDoubleOrNull() ?: 0.0,
                            dxyPrice = dxyRes?.price?.toDoubleOrNull() ?: 0.0,
                            dxyChange = dxyRes?.changePercent?.replace("%", "")?.toDoubleOrNull() ?: 0.0,
                            btcSp500Correlation = 0.0, // Calculated separately
                            btcGoldCorrelation = 0.0,
                            timestamp = System.currentTimeMillis(),
                        )
                    Result.success(data)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun getCalendarEvents(): Result<List<CalendarEvent>> = Result.success(emptyList())

        override suspend fun getMacroCorrelations(): Result<List<MacroCorrelation>> =
            coroutineScope {
                try {
                    // Get BTC daily history (mocked or from repo if available, for now use AlphaVantage for BTC too to align dates)
                    val btcHistory = async { getAssetTimeSeries("BTCUSD") }.await().getOrThrow()

                    val assets = listOf("SPY" to "S&P 500", "GLD" to "GOLD", "UUP" to "DXY")
                    val correlations =
                        assets.map { (symbol, name) ->
                            val assetHistory = getAssetTimeSeries(symbol).getOrNull() ?: emptyList()
                            val correlation = calculateCorrelation(btcHistory, assetHistory)

                            MacroCorrelation(
                                asset = name,
                                correlation = correlation,
                                strength = getCorrelationStrength(correlation),
                                description = getCorrelationDescription(name, correlation),
                                lastPrice = assetHistory.lastOrNull()?.price ?: 0.0,
                                change24h = 0.0, // Placeholder
                            )
                        }
                    Result.success(correlations)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

        override suspend fun getAssetTimeSeries(symbol: String): Result<List<MacroDataPoint>> =
            try {
                val response = alphaVantageApi.getTimeSeriesDaily(symbol = symbol)
                val points =
                    response.timeSeries
                        ?.map { (date, dto) ->
                            MacroDataPoint(date, dto.close.toDoubleOrNull() ?: 0.0)
                        }?.sortedBy { it.date } ?: emptyList()
                Result.success(points)
            } catch (e: Exception) {
                Result.failure(e)
            }

        private fun calculateCorrelation(
            list1: List<MacroDataPoint>,
            list2: List<MacroDataPoint>,
        ): Double {
            val map2 = list2.associateBy { it.date }
            val common = list1.filter { map2.containsKey(it.date) }
            if (common.size < 5) return 0.0

            val x = common.map { it.price }
            val y = common.map { map2[it.date]!!.price }

            val n = x.size
            val sumX = x.sum()
            val sumY = y.sum()
            val sumX2 = x.sumOf { it * it }
            val sumY2 = y.sumOf { it * it }
            val sumXY = x.zip(y).sumOf { it.first * it.second }

            val numerator = n * sumXY - sumX * sumY
            val denominator = sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))

            return if (denominator != 0.0) numerator / denominator else 0.0
        }

        private fun getCorrelationStrength(c: Double) =
            when {
                c > 0.7 -> "STRONG_POSITIVE"
                c > 0.3 -> "POSITIVE"
                c < -0.7 -> "STRONG_INVERSE"
                c < -0.3 -> "INVERSE"
                else -> "DECOUPLED"
            }

        private fun getCorrelationDescription(
            asset: String,
            c: Double,
        ) = when {
            c > 0.7 -> "BTC moves in lockstep with $asset. Macro liquidity dominates."
            c < -0.7 -> "BTC is strongly inverse to $asset. Acting as a hedge."
            abs(c) < 0.2 -> "BTC is decoupled from $asset. Idiosyncratic crypto moves."
            else -> "Moderate relationship with $asset detected."
        }

        private fun abs(n: Double) = if (n < 0) -n else n
    }
