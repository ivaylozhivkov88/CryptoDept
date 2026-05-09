package com.cryptodept.data.repository

import com.cryptodept.data.api.BinanceFuturesApi
import com.cryptodept.data.api.BinanceWebSocketManager
import com.cryptodept.data.api.CoinGeckoApi
import com.cryptodept.data.api.KrakenWebSocketManager
import com.cryptodept.data.api.MultiSourcePriceAggregator
import com.cryptodept.data.billing.BillingManager
import com.cryptodept.data.db.CoinDao
import com.cryptodept.data.db.CoinEntity
import com.cryptodept.data.db.PriceHistoryDao
import com.cryptodept.domain.model.CryptoResult
import com.cryptodept.domain.repository.AlertsRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CryptoRepositoryTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var api: CoinGeckoApi
    private lateinit var repository: CryptoRepositoryImpl

    // Mocks
    private val aggregator = mockk<MultiSourcePriceAggregator>(relaxed = true)
    private val coinDao = mockk<CoinDao>(relaxed = true)
    private val priceHistoryDao = mockk<PriceHistoryDao>(relaxed = true)
    private val binanceWS = mockk<BinanceWebSocketManager>(relaxed = true)
    private val krakenWS = mockk<KrakenWebSocketManager>(relaxed = true)
    private val binanceApi = mockk<BinanceFuturesApi>(relaxed = true)
    private val billingManager = mockk<BillingManager>(relaxed = true)
    private val alertsRepository = mockk<AlertsRepository>(relaxed = true)

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        api =
            Retrofit
                .Builder()
                .baseUrl(mockWebServer.url("/"))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CoinGeckoApi::class.java)

        every { billingManager.isPro } returns MutableStateFlow(false)
        every { coinDao.getAllCoins() } returns flowOf(emptyList())

        repository =
            CryptoRepositoryImpl(
                api,
                aggregator,
                coinDao,
                priceHistoryDao,
                binanceWS,
                krakenWS,
                binanceApi,
                billingManager,
            )
        repository.alertsRepository = alertsRepository
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun getCoinDetail_success_returnsSuccessWithData() =
        runBlocking {
            val json =
                """
                {
                    "id": "bitcoin",
                    "symbol": "btc",
                    "name": "Bitcoin",
                    "description": {"en": "Digital gold"},
                    "market_data": {
                        "current_price": {"usd": 60000.0},
                        "market_cap": {"usd": 1200000000.0},
                        "total_volume": {"usd": 30000000.0},
                        "price_change_percentage_24h": 2.5
                    }
                }
                """.trimIndent()

            mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))

            val result = repository.getCoinDetail("bitcoin")

            assertThat(result is CryptoResult.Success).isTrue()
            val detail = (result as CryptoResult.Success).data
            assertThat(detail.id).isEqualTo("bitcoin")
            assertThat(detail.currentPrice).isEqualTo(60000.0)
        }

    @Test
    fun getCoinDetail_error_returnsError() =
        runBlocking {
            mockWebServer.enqueue(MockResponse().setResponseCode(404))

            val result = repository.getCoinDetail("unknown")

            assertThat(result is CryptoResult.Error).isTrue()
        }

    @Test
    fun getGlobalMarketData_success_returnsData() =
        runBlocking {
            val json =
                """
                {
                    "data": {
                        "active_cryptocurrencies": 10000,
                        "total_market_cap": {"usd": 2500000000000.0},
                        "total_volume": {"usd": 50000000000.0},
                        "market_cap_percentage": {"btc": 50.0, "eth": 18.0},
                        "market_cap_change_percentage_24h_usd": -1.2
                    }
                }
                """.trimIndent()

            mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))

            val result = repository.getGlobalMarketData()

            assertThat(result is CryptoResult.Success).isTrue()
            val data = (result as CryptoResult.Success).data
            assertThat(data.activeCoins).isEqualTo(10000)
            assertThat(data.btcDominance).isEqualTo(50.0)
        }

    @Test
    fun toggleTracking_success_updatesDAO() =
        runBlocking {
            val coin =
                CoinEntity(
                    id = "bitcoin",
                    symbol = "btc",
                    name = "Bitcoin",
                    isTracked = false,
                    currentPrice = 60000.0,
                    priceChange24h = 0.0,
                    priceChangePercentage24h = 0.0,
                    marketCap = 0.0,
                    totalVolume = 0.0,
                    high24h = 0.0,
                    low24h = 0.0,
                    lastUpdated = 0,
                    sourcesCount = 1,
                    maxDeviation = 0.0,
                )
            coEvery { coinDao.getCoinById("bitcoin") } returns coin
            coEvery { coinDao.getTrackedCoinsCount() } returns 1

            val result = repository.toggleTracking("bitcoin")

            assertThat(result is CryptoResult.Success).isTrue()
            coVerify { coinDao.updateCoin(any()) }
        }

    @Test
    fun toggleTracking_failForLimitReached_returnsError() =
        runBlocking {
            val coin =
                CoinEntity(
                    id = "newcoin",
                    symbol = "nc",
                    name = "NewCoin",
                    isTracked = false,
                    currentPrice = 1.0,
                    priceChange24h = 0.0,
                    priceChangePercentage24h = 0.0,
                    marketCap = 0.0,
                    totalVolume = 0.0,
                    high24h = 0.0,
                    low24h = 0.0,
                    lastUpdated = 0,
                    sourcesCount = 1,
                    maxDeviation = 0.0,
                )
            coEvery { coinDao.getCoinById("newcoin") } returns coin
            coEvery { coinDao.getTrackedCoinsCount() } returns 3 // Max for free users
            every { billingManager.isPro.value } returns false

            val result = repository.toggleTracking("newcoin")

            assertThat(result is CryptoResult.Error).isTrue()
            val error = (result as CryptoResult.Error)
            assertThat(error.throwable.message).isEqualTo("PRO_REQUIRED_LIMIT_3")
        }

    @Test
    fun refreshPrices_success_insertsCoins() =
        runBlocking {
            val json =
                """
                [
                    {
                        "id": "bitcoin",
                        "symbol": "btc",
                        "name": "Bitcoin",
                        "current_price": 60000.0,
                        "market_cap": 1000000000.0,
                        "total_volume": 20000000.0,
                        "high_24h": 61000.0,
                        "low_24h": 59000.0,
                        "price_change_24h": 1000.0,
                        "price_change_percentage_24h": 1.5
                    }
                ]
                """.trimIndent()

            mockWebServer.enqueue(MockResponse().setBody(json).setResponseCode(200))
            coEvery { aggregator.fetchAggregatedPrice(any(), any()) } returns
                mockk(relaxed = true) {
                    every { consensusPrice } returns 60000.0
                }

            val result = repository.refreshPrices()

            assertThat(result is CryptoResult.Success).isTrue()
            coVerify { coinDao.insertCoins(any()) }
        }

    @Test
    fun refreshPrices_rateLimit_doesNotCallAPI() =
        runBlocking {
            // First call sets lastFetchTime
            mockWebServer.enqueue(MockResponse().setBody("[]").setResponseCode(200))
            repository.refreshPrices()

            // Second call immediately after should skip
            val result = repository.refreshPrices()

            assertThat(result is CryptoResult.Success).isTrue()
            assertThat(mockWebServer.requestCount).isEqualTo(1)
        }

    @Test
    fun refreshPrices_networkError_returnsError() =
        runBlocking {
            mockWebServer.enqueue(MockResponse().setResponseCode(500))

            val result = repository.refreshPrices()

            assertThat(result is CryptoResult.Error).isTrue()
            val error = result as CryptoResult.Error
            assertThat(error.code).isEqualTo(500)
        }
}
