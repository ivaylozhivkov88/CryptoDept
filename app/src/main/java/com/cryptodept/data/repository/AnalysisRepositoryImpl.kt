package com.cryptodept.data.repository

import com.cryptodept.data.db.PriceHistoryDao
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.AnalysisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

@Singleton
class AnalysisRepositoryImpl @Inject constructor(
    private val priceHistoryDao: PriceHistoryDao
) : AnalysisRepository {

    override fun getTechnicalIndicators(coinId: String): Flow<TechnicalIndicators> {
        return priceHistoryDao.getPriceHistory(coinId).map { history ->
            // Обръщаме историята, за да работим в хронологичен ред (от минало към настояще)
            val prices = history.map { it.price }.reversed()

            if (prices.isEmpty()) {
                return@map TechnicalIndicators.default()
            }

            TechnicalIndicators(
                rsi = calculateRSI(prices),
                macd = calculateMACD(prices),
                bollingerBands = calculateBollingerBands(prices),
                emas = calculateEMAs(prices, listOf(9, 21, 50, 200)),
                trend = determineTrend(prices),
                supportLevels = findSupportLevels(prices),
                resistanceLevels = findResistanceLevels(prices)
            )
        }
    }

    private fun calculateRSI(prices: List<Double>, period: Int = 14): Float {
        if (prices.size <= period) return 50f

        var gains = 0.0
        var losses = 0.0

        // Първоначално изчисление на средна печалба/загуба
        for (i in 1..period) {
            val diff = prices[prices.size - period + i - 1] - prices[prices.size - period + i - 2]
            if (diff >= 0) gains += diff else losses -= diff
        }

        val avgGain = gains / period
        val avgLoss = losses / period

        if (avgLoss == 0.0) return 100f

        val rs = avgGain / avgLoss
        return (100 - (100 / (1 + rs))).toFloat()
    }

    private fun calculateMACD(prices: List<Double>): MACDData {
        if (prices.size < 26) return MACDData(0f, 0f, 0f)

        val ema12 = calculateEMA(prices, 12)
        val ema26 = calculateEMA(prices, 26)
        val macdLine = (ema12 - ema26).toFloat()

        // За коректен Signal Line и Histogram е необходима MACD история.
        // За целите на UI тук подаваме macdLine и като хистограма.
        return MACDData(
            macdLine = macdLine,
            signalLine = 0f,
            histogram = macdLine
        )
    }

    private fun calculateBollingerBands(prices: List<Double>, period: Int = 20): BollingerBandsData {
        if (prices.size < period) {
            val lastPrice = prices.lastOrNull() ?: 0.0
            return BollingerBandsData(lastPrice, lastPrice, lastPrice)
        }

        val lastPeriod = prices.takeLast(period)
        val sma = lastPeriod.average()
        val variance = lastPeriod.map { (it - sma).pow(2) }.sum() / period
        val stdDev = sqrt(variance)

        return BollingerBandsData(
            upper = sma + 2 * stdDev,
            middle = sma,
            lower = sma - 2 * stdDev
        )
    }

    private fun calculateEMA(prices: List<Double>, period: Int): Double {
        if (prices.isEmpty()) return 0.0
        if (prices.size < period) return prices.last()

        val k = 2.0 / (period + 1)
        var ema = prices.first() // Започваме от първата цена като начална стойност
        for (i in 1 until prices.size) {
            ema = prices[i] * k + ema * (1 - k)
        }
        return ema
    }

    private fun calculateEMAs(prices: List<Double>, periods: List<Int>): Map<Int, Double> {
        return periods.associateWith { calculateEMA(prices, it) }
    }

    private fun determineTrend(prices: List<Double>): TrendSignal {
        if (prices.size < 50) return TrendSignal.NEUTRAL

        val ema50 = calculateEMA(prices, 50)
        val currentPrice = prices.last()

        val isUpward = currentPrice > ema50
        val isStrong = if (prices.size >= 3) {
            val last3 = prices.takeLast(3)
            if (isUpward) last3[2] > last3[1] && last3[1] > last3[0]
            else last3[2] < last3[1] && last3[1] < last3[0]
        } else false

        return when {
            isUpward && isStrong -> TrendSignal.STRONG_BULLISH
            isUpward -> TrendSignal.BULLISH
            !isUpward && isStrong -> TrendSignal.STRONG_BEARISH
            else -> TrendSignal.BEARISH
        }
    }

    private fun findSupportLevels(prices: List<Double>): List<Double> {
        if (prices.isEmpty()) return emptyList()
        // Връщаме най-ниската цена от последните 100 периода като основна подкрепа
        return listOf(prices.takeLast(100).minOrNull() ?: prices.last())
    }

    private fun findResistanceLevels(prices: List<Double>): List<Double> {
        if (prices.isEmpty()) return emptyList()
        // Връщаме най-високата цена от последните 100 периода като основна съпротива
        return listOf(prices.takeLast(100).maxOrNull() ?: prices.last())
    }
}