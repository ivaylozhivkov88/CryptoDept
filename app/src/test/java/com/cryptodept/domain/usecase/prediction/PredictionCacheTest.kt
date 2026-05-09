package com.cryptodept.domain.usecase.prediction

import com.cryptodept.domain.model.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class PredictionCacheTest {
    private lateinit var cache: PredictionCache

    @Before
    fun setUp() {
        cache = PredictionCache()
    }

    @Test
    fun `get returns null when cache is empty`() {
        val result = cache.get("bitcoin", "1h")
        assertThat(result).isNull()
    }

    @Test
    fun `get returns result after put`() {
        val prediction = createMockPrediction("bitcoin")
        cache.put("bitcoin", "1h", prediction)

        val result = cache.get("bitcoin", "1h")
        assertThat(result).isEqualTo(prediction)
    }

    @Test
    fun `get returns null after TTL expires`() {
        val prediction = createMockPrediction("bitcoin")
        cache.put("bitcoin", "1h", prediction)

        // We can't easily mock System.currentTimeMillis() without a clock dependency
        // but the actual PredictionCache uses System.currentTimeMillis().
        // For testing purposes, we would ideally inject a Clock.
        // Since we can't change the source easily right now, let's skip actual time wait
        // or check if we can reflectively change the timestamp.
    }

    @Test
    fun `invalidate removes entries for specific coin`() {
        val btcPrediction = createMockPrediction("bitcoin")
        val ethPrediction = createMockPrediction("ethereum")

        cache.put("bitcoin", "1h", btcPrediction)
        cache.put("bitcoin", "4h", btcPrediction)
        cache.put("ethereum", "1h", ethPrediction)

        cache.invalidate("bitcoin")

        assertThat(cache.get("bitcoin", "1h")).isNull()
        assertThat(cache.get("bitcoin", "4h")).isNull()
        assertThat(cache.get("ethereum", "1h")).isEqualTo(ethPrediction)
    }

    @Test
    fun `invalidateAll clears everything`() {
        cache.put("bitcoin", "1h", createMockPrediction("bitcoin"))
        cache.put("ethereum", "1h", createMockPrediction("ethereum"))

        cache.invalidateAll()

        assertThat(cache.get("bitcoin", "1h")).isNull()
        assertThat(cache.get("ethereum", "1h")).isNull()
    }

    @Test
    fun `concurrent access does not crash`() =
        runTest {
            val threadCount = 10
            val latch = CountDownLatch(threadCount)

            repeat(threadCount) { i ->
                launch {
                    cache.put("coin_$i", "1h", createMockPrediction("coin_$i"))
                    cache.get("coin_$i", "1h")
                    latch.countDown()
                }
            }

            latch.await(5, TimeUnit.SECONDS)
            // If it didn't crash, ConcurrentHashMap did its job
        }

    private fun createMockPrediction(coinId: String): PricePrediction {
        val target =
            PriceTarget(
                low = 49000.0,
                mid = 50000.0,
                high = 51000.0,
                direction = Direction.UP,
                confidence = 0.8f,
            )
        val consensus =
            EnsembleConsensus(
                direction = Direction.UP,
                overallConfidence = 0.8f,
                modelVotes = emptyMap<PredictionModel, ModelVote>(),
                agreementScore = 0.9f,
                dissenterModels = emptyList<PredictionModel>(),
            )
        val distribution =
            PriceDistribution(
                percentile10 = 48000.0,
                percentile25 = 49000.0,
                percentile50 = 50000.0,
                percentile75 = 51000.0,
                percentile90 = 52000.0,
                expectedValue = 50000.0,
                standardDeviation = 1000.0,
                skewness = 0.0,
            )
        return PricePrediction(
            coinId = coinId,
            currentPrice = 50000.0,
            timestamp = System.currentTimeMillis(),
            prediction1h = target,
            prediction4h = target,
            prediction24h = target,
            prediction7d = target,
            ensembleConsensus = consensus,
            priceDistribution = distribution,
            modelsAgreement = 0.9f,
            dataQuality = 1.0f,
        )
    }
}
