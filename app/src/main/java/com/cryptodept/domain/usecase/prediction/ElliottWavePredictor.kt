package com.cryptodept.domain.usecase.prediction

import com.cryptodept.domain.model.Direction
import com.cryptodept.domain.model.ModelVote
import com.cryptodept.domain.model.PredictionModel
import javax.inject.Inject
import kotlin.math.abs

class ElliottWavePredictor @Inject constructor() {

    data class PivotPoint(
        val price: Double,
        val index: Int,
        val isHigh: Boolean
    )

    fun predict(closes: List<Double>): ModelVote {
        // Изискваме минимум 50 свещи за адекватен вълнов анализ
        if (closes.size < 50) return neutralVote()

        val pivots = findPivots(closes)
        // За разпознаване на базова 5-вълнова структура ни трябват поне 5 пивота
        if (pivots.size < 5) return neutralVote()

        val lastPivots = pivots.takeLast(5)
        val direction = analyzeWaveStructure(lastPivots)

        val currentPrice = closes.last()

        // Изчисляваме целева цена на базата на стандартни Fibonacci разширения (прост модел)
        val targetPrice = when (direction) {
            Direction.STRONG_UP, Direction.UP -> currentPrice * 1.05
            Direction.STRONG_DOWN, Direction.DOWN -> currentPrice * 0.95
            else -> currentPrice
        }

        return ModelVote(
            model = PredictionModel.ELLIOTT_WAVE,
            direction = direction,
            targetPrice = targetPrice,
            confidence = 0.65f, // Elliott Wave винаги има елемент на субективност
            weight = PredictionModel.ELLIOTT_WAVE.baseWeight
        )
    }

    private fun findPivots(prices: List<Double>): List<PivotPoint> {
        val pivots = mutableListOf<PivotPoint>()
        val window = 5 // Прозорец за локален екстремум

        for (i in window until prices.size - window) {
            val current = prices[i]
            val range = prices.subList(i - window, i + window + 1)

            if (current == range.maxOrNull()) {
                pivots.add(PivotPoint(current, i, true))
            } else if (current == range.minOrNull()) {
                pivots.add(PivotPoint(current, i, false))
            }
        }
        return pivots
    }

    private fun analyzeWaveStructure(pivots: List<PivotPoint>): Direction {
        // Проверка за висши върхове и висши дъна (Bullish Trend)
        val isBullish = pivots.zipWithNext().all { (a, b) ->
            when {
                a.isHigh && b.isHigh -> b.price > a.price
                !a.isHigh && !b.isHigh -> b.price > a.price
                else -> true
            }
        }

        // Проверка за по-ниски върхове и по-ниски дъна (Bearish Trend)
        val isBearish = pivots.zipWithNext().all { (a, b) ->
            when {
                a.isHigh && b.isHigh -> b.price < a.price
                !a.isHigh && !b.isHigh -> b.price < a.price
                else -> true
            }
        }

        return when {
            isBullish -> Direction.STRONG_UP
            isBearish -> Direction.STRONG_DOWN
            else -> Direction.SIDEWAYS
        }
    }

    private fun neutralVote() = ModelVote(
        model = PredictionModel.ELLIOTT_WAVE,
        direction = Direction.SIDEWAYS,
        targetPrice = 0.0,
        confidence = 0f,
        weight = PredictionModel.ELLIOTT_WAVE.baseWeight
    )
}