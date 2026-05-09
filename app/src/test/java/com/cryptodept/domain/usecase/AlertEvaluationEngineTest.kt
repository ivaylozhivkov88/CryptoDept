package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertEvaluationEngineTest {
    private val engine = AlertEvaluationEngine()

    private fun createSnapshot(
        price: Double,
        rsi: Double = 50.0,
        volMult: Double = 1.0,
    ): TechnicalSnapshot =
        TechnicalSnapshot(
            coinSymbol = "BTC",
            price = price,
            priceChange24h = 0.0,
            rsi = RSIIndicator(rsi.toFloat()),
            volume =
                VolumeIndicator(
                    volumeChange24h = 0.0,
                    volumeAvg14 = 1000.0,
                    currentVolume = 1000.0 * volMult,
                    volumeMultiplier = volMult,
                ),
            macd = null,
        )

    @Test
    fun `evaluate price above triggers correctly`() {
        val alert =
            CompositeAlert(
                name = "Price Alert",
                coinId = "bitcoin",
                coinSymbol = "BTC",
                conditions =
                    listOf(
                        AlertCondition(
                            type = AlertConditionType.PRICE,
                            operator = AlertDirection.ABOVE,
                            targetValue = 50000.0,
                            description = "",
                        ),
                    ).toImmutableList(),
            )

        assertTrue(engine.evaluate(alert, createSnapshot(price = 51000.0)).overallResult)
        assertFalse(engine.evaluate(alert, createSnapshot(price = 49000.0)).overallResult)
    }

    @Test
    fun `evaluate AND logic with price and rsi`() {
        val alert =
            CompositeAlert(
                name = "AND Alert",
                coinId = "bitcoin",
                coinSymbol = "BTC",
                logicOperator = AlertLogicOperator.AND,
                conditions =
                    listOf(
                        AlertCondition(
                            type = AlertConditionType.PRICE,
                            operator = AlertDirection.ABOVE,
                            targetValue = 50000.0,
                            description = "",
                        ),
                        AlertCondition(
                            type = AlertConditionType.RSI,
                            operator = AlertDirection.BELOW,
                            targetValue = 30.0,
                            description = "",
                        ),
                    ).toImmutableList(),
            )

        // Both true
        assertTrue(engine.evaluate(alert, createSnapshot(price = 51000.0, rsi = 25.0)).overallResult)
        // Price false, RSI true
        assertFalse(engine.evaluate(alert, createSnapshot(price = 49000.0, rsi = 25.0)).overallResult)
        // Price true, RSI false
        assertFalse(engine.evaluate(alert, createSnapshot(price = 51000.0, rsi = 40.0)).overallResult)
    }

    @Test
    fun `evaluate OR logic with price and volume`() {
        val alert =
            CompositeAlert(
                name = "OR Alert",
                coinId = "bitcoin",
                coinSymbol = "BTC",
                logicOperator = AlertLogicOperator.OR,
                conditions =
                    listOf(
                        AlertCondition(
                            type = AlertConditionType.PRICE,
                            operator = AlertDirection.ABOVE,
                            targetValue = 70000.0,
                            description = "",
                        ),
                        AlertCondition(
                            type = AlertConditionType.VOLUME,
                            operator = AlertDirection.ABOVE,
                            targetValue = 2.0,
                            description = "",
                        ),
                    ).toImmutableList(),
            )

        // Only price true
        assertTrue(engine.evaluate(alert, createSnapshot(price = 71000.0, volMult = 1.0)).overallResult)
        // Only volume true
        assertTrue(engine.evaluate(alert, createSnapshot(price = 60000.0, volMult = 2.5)).overallResult)
        // Both false
        assertFalse(engine.evaluate(alert, createSnapshot(price = 69000.0, volMult = 1.5)).overallResult)
    }

    @Test
    fun `evaluate empty conditions returns false`() {
        val alert =
            CompositeAlert(
                name = "Empty Alert",
                coinId = "bitcoin",
                coinSymbol = "BTC",
                conditions = persistentListOf(),
            )
        assertFalse(engine.evaluate(alert, createSnapshot(price = 50000.0)).overallResult)
    }

    @Test
    fun `evaluate RSI conditions correctly`() {
        val alert =
            CompositeAlert(
                name = "RSI Alert",
                coinId = "bitcoin",
                coinSymbol = "BTC",
                conditions =
                    listOf(
                        AlertCondition(
                            type = AlertConditionType.RSI,
                            operator = AlertDirection.BELOW,
                            targetValue = 30.0,
                            description = "",
                        ),
                    ).toImmutableList(),
            )
        assertTrue(engine.evaluate(alert, createSnapshot(price = 50000.0, rsi = 25.0)).overallResult)
        assertFalse(engine.evaluate(alert, createSnapshot(price = 50000.0, rsi = 35.0)).overallResult)
    }

    @Test
    fun `stress test with 100 alerts`() {
        val alerts =
            List(100) { i ->
                CompositeAlert(
                    id = i,
                    name = "Alert $i",
                    coinId = "bitcoin",
                    coinSymbol = "BTC",
                    logicOperator = if (i % 2 == 0) AlertLogicOperator.AND else AlertLogicOperator.OR,
                    conditions =
                        listOf(
                            AlertCondition(
                                type = AlertConditionType.PRICE,
                                operator = AlertDirection.ABOVE,
                                targetValue = 50000.0 + i,
                                description = "",
                            ),
                            AlertCondition(
                                type = AlertConditionType.RSI,
                                operator = AlertDirection.BELOW,
                                targetValue = 30.0,
                                description = "",
                            ),
                        ).toImmutableList(),
                )
            }

        val snapshot = createSnapshot(price = 60000.0, rsi = 25.0)
        val startTime = System.nanoTime()
        val results = alerts.map { engine.evaluate(it, snapshot) }
        val endTime = System.nanoTime()

        val totalTimeMs = (endTime - startTime) / 1_000_000.0
        println("Evaluated 100 alerts in ${totalTimeMs}ms")

        assertTrue(results.size == 100)
        // Each evaluation should be very fast
        assertTrue("Total time for 100 alerts should be < 50ms on modern CPU", totalTimeMs < 50.0)
    }

    @Test
    fun `performance benchmark for complex alert`() {
        val complexAlert =
            CompositeAlert(
                name = "Complex Alert",
                coinId = "bitcoin",
                coinSymbol = "BTC",
                logicOperator = AlertLogicOperator.AND,
                conditions =
                    listOf(
                        AlertCondition(
                            type = AlertConditionType.PRICE,
                            operator = AlertDirection.ABOVE,
                            targetValue = 50000.0,
                            description = "",
                        ),
                        AlertCondition(
                            type = AlertConditionType.RSI,
                            operator = AlertDirection.BELOW,
                            targetValue = 30.0,
                            description = "",
                        ),
                        AlertCondition(
                            type = AlertConditionType.VOLUME,
                            operator = AlertDirection.ABOVE,
                            targetValue = 1.5,
                            description = "",
                        ),
                        AlertCondition(
                            type = AlertConditionType.MACD,
                            operator = AlertDirection.ABOVE,
                            targetValue = 0.0,
                            description = "",
                        ),
                        AlertCondition(
                            type = AlertConditionType.PRICE,
                            operator = AlertDirection.BELOW,
                            targetValue = 100000.0,
                            description = "",
                        ),
                    ).toImmutableList(),
            )

        val snapshot = createSnapshot(price = 60000.0, rsi = 25.0, volMult = 2.0)

        // Warmup
        repeat(100) { engine.evaluate(complexAlert, snapshot) }

        val startTime = System.nanoTime()
        repeat(1000) { engine.evaluate(complexAlert, snapshot) }
        val endTime = System.nanoTime()

        val avgTimeNs = (endTime - startTime) / 1000.0
        val avgTimeMs = avgTimeNs / 1_000_000.0
        println("Average evaluation time for 5-condition alert: ${avgTimeMs}ms")

        assertTrue("Evaluation of a single complex alert should be < 1ms", avgTimeMs < 1.0)
    }
}
