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
            val prices = history.map { it.price }.reversed()
            
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
        
        for (i in 1..period) {
            val diff = prices[prices.size - period + i] - prices[prices.size - period + i - 1]
            if (diff >= 0) gains += diff else losses -= diff
        }
        
        var avgGain = gains / period
        var avgLoss = losses / period
        
        if (avgLoss == 0.0) return 100f
        
        val rs = avgGain / avgLoss
        return (100 - (100 / (1 + rs))).toFloat()
    }

    private fun calculateMACD(prices: List<Double>): MACDData {
        val ema12 = calculateEMA(prices, 12)
        val ema26 = calculateEMA(prices, 26)
        val macdLine = (ema12 - ema26).toFloat()
        
        // This is a simplified MACD calculation for the last point
        // Real MACD Signal line requires a history of MACD values
        return MACDData(macdLine, 0f, macdLine) 
    }

    private fun calculateBollingerBands(prices: List<Double>, period: Int = 20): BollingerBandsData {
        if (prices.size < period) return BollingerBandsData(0.0, 0.0, 0.0)
        
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
        val k = 2.0 / (period + 1)
        var ema = prices[0]
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
        
        return if (currentPrice > ema50) {
            if (prices.takeLast(3).let { it[2] > it[1] && it[1] > it[0] }) TrendSignal.STRONG_BULLISH
            else TrendSignal.BULLISH
        } else {
            if (prices.takeLast(3).let { it[2] < it[1] && it[1] < it[0] }) TrendSignal.STRONG_BEARISH
            else TrendSignal.BEARISH
        }
    }

    private fun findSupportLevels(prices: List<Double>): List<Double> {
        if (prices.size < 10) return emptyList()
        // Simplified support detection: local minima in windows
        return listOf(prices.minOrNull() ?: 0.0)
    }

    private fun findResistanceLevels(prices: List<Double>): List<Double> {
        if (prices.size < 10) return emptyList()
        // Simplified resistance detection: local maxima
        return listOf(prices.maxOrNull() ?: 0.0)
    }
}