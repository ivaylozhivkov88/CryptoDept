package com.cryptodept.data.repository

import com.cryptodept.data.api.*
import com.cryptodept.data.billing.BillingService
import com.cryptodept.data.db.CoinDao
import com.cryptodept.data.db.NetworkHealthDao
import com.cryptodept.domain.repository.AlertsRepository
import com.cryptodept.domain.repository.PriceHistoryRepository
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
    private val cgSource = mockk<CoinGeckoPriceSource>(relaxed = true)
    private val binanceSource = mockk<BinancePriceSource>(relaxed = true)
    private val aggregator = mockk<MultiSourcePriceAggregator>(relaxed = true)
    private val coinDao = mockk<CoinDao>(relaxed = true)
    private val priceHistoryRepository = mockk<PriceHistoryRepository>(relaxed = true)
    private val networkHealthDao = mockk<NetworkHealthDao>(relaxed = true)
    private val binanceWS = mockk<BinanceWebSocketService>(relaxed = true)
    private val krakenWS = mockk<KrakenWebSocketService>(relaxed = true)
    private val billingService = mockk<BillingService>(relaxed = true)
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

        every { billingService.isPro } returns MutableStateFlow(false)
        every { coinDao.getTrackedCoins() } returns flowOf(emptyList())

        repository =
            CryptoRepositoryImpl(
                api,
                cgSource,
                binanceSource,
                aggregator,
                coinDao,
                priceHistoryRepository,
                networkHealthDao,
                binanceWS,
                krakenWS
            )
        repository.alertsRepository = alertsRepository
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `getCoinDetail success returns success with data`() {
        // Test implementation
    }
}
