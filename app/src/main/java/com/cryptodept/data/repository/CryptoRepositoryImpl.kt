package com.cryptodept.data.repository

import android.util.Log
import com.cryptodept.data.api.*
import com.cryptodept.data.db.CoinDao
import com.cryptodept.data.db.CoinEntity
import com.cryptodept.data.db.PriceHistoryDao
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.* // Глобален импорт на интерфейсите
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoRepositoryImpl @Inject constructor(
    private val api: CoinGeckoApi,
    private val aggregator: MultiSourcePriceAggregator,
    private val coinDao: CoinDao,
    private val priceHistoryDao: PriceHistoryDao,
    private val binanceWS: BinanceWebSocketManager,
    private val krakenWS: KrakenWebSocketManager,
    private val binanceApi: BinanceFuturesApi,
    private val billingManager: com.cryptodept.data.billing.BillingManager
) : CryptoRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastFetchTime = 0L
    private val RATE_LIMIT_MS = 10_000L
    private var subscriptionJob: Job? = null

    /**
     * WebSocket subscriptions are managed by SocketLifecycleManager.
     * This repository responds to real-time price updates through alert checks.
     * @see com.cryptodept.service.SocketLifecycleManager
     */
    internal fun startPriceSubscriptions() {
        if (subscriptionJob?.isActive == true) {
            Log.d("CryptoDept_Repo", "Price subscriptions already active")
            return
        }

        subscriptionJob = repositoryScope.launch {
            try {
                // Binance WebSocket Stream
                launch {
                    binanceWS.observeTickerStream().collect { ticker ->
                        val coinId = when(ticker.symbol) {
                            "BTCUSDT" -> "bitcoin"
                            "ETHUSDT" -> "ethereum"
                            "XRPUSDT" -> "ripple"
                            "SOLUSDT" -> "solana"
                            else -> null
                        }
                        coinId?.let {
                            updateConsensusPrice(it, "binance", ticker.lastPrice.toDouble())
                        }
                    }
                }

                // Kraken WebSocket Stream
                launch {
                    krakenWS.observeTickerStream().collect { (coinId, price) ->
                        updateConsensusPrice(coinId, "kraken", price)
                    }
                }
            } catch (e: Exception) {
                Log.e("CryptoDept_Repo", "Error in price subscriptions", e)
            }
        }
        Log.d("CryptoDept_Repo", "Price subscriptions started")
    }

    internal fun stopPriceSubscriptions() {
        subscriptionJob?.cancel()
        subscriptionJob = null
        Log.d("CryptoDept_Repo", "Price subscriptions stopped")
    }

    @Inject
    lateinit var alertsRepository: AlertsRepository

    private suspend fun updateConsensusPrice(coinId: String, source: String, price: Double) {
        try {
            // Актуализираме агрегатора и вземаме консенсусната цена
            val aggPrice = aggregator.updatePriceFromWS(coinId, source, price)

            // Записваме в базата само ако имаме реална промяна или нов консенсус
            coinDao.updatePrice(
                id = coinId,
                newPrice = aggPrice.consensusPrice,
                sourcesCount = aggPrice.sourcesCount,
                deviation = aggPrice.maxDeviationPercent,
                timestamp = System.currentTimeMillis()
            )

            // ПРОВЕРКА ЗА АЛЕРТИ В РЕАЛНО ВРЕМЕ
            try {
                alertsRepository.checkAlerts(coinId, aggPrice.consensusPrice)
            } catch (e: Exception) {
                Log.e("CryptoDept_Repo", "Error checking alerts for $coinId: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("CryptoDept_Repo", "Error updating consensus price for $coinId from $source", e)
        }
    }

    override fun getTrackedCoinPrices(): Flow<List<CoinPrice>> {
        return coinDao.getTrackedCoins().map { entities ->
            entities.map { it.toDomainPrice() }
        }
    }

    override suspend fun refreshPrices(): Result<Unit> = coroutineScope {
        try {
            // Проверка за Rate Limit (CoinGecko Free API е силно ограничено)
            if (System.currentTimeMillis() - lastFetchTime < RATE_LIMIT_MS) {
                return@coroutineScope Result.success(Unit)
            }

            // --- REFACTORED FOR PRO SUPREME MODE ---
            val isPro = billingManager.isPro.value
            val trackedFromDb = coinDao.getTrackedCoins().first().map { it.id }
            
            val baseIds = listOf(
                "bitcoin", "ethereum", "ripple", "cardano", "solana",
                "polkadot", "dogecoin", "chainlink", "shiba-inu", "litecoin",
                "avalanche-2", "tron", "matic-network", "stellar", "cosmos"
            )
            
            // Combine base + user tracked, unique
            val allIds = (baseIds + trackedFromDb).distinct()
            val idsToFetch = if (isPro) allIds else baseIds.take(15)
            
            val idsString = idsToFetch.joinToString(",")

            val marketResponse = api.getCoinMarkets(ids = idsString)
            lastFetchTime = System.currentTimeMillis()

            val entities = marketResponse.map { res ->
                // Първоначална агрегация с цената от CoinGecko
                val aggPrice = aggregator.fetchAggregatedPrice(res.id, res.current_price)

                CoinEntity(
                    id = res.id,
                    symbol = res.symbol,
                    name = res.name,
                    isTracked = true,
                    currentPrice = aggPrice.consensusPrice,
                    priceChange24h = res.price_change_24h,
                    priceChangePercentage24h = res.price_change_percentage_24h,
                    marketCap = res.market_cap,
                    totalVolume = res.total_volume,
                    high24h = res.high_24h,
                    low24h = res.low_24h,
                    lastUpdated = System.currentTimeMillis(),
                    sourcesCount = aggPrice.sourcesCount,
                    maxDeviation = aggPrice.maxDeviationPercent
                )
            }
            coinDao.insertCoins(entities)
            Result.success(Unit)
        } catch (e: HttpException) {
            Log.e("CryptoRepository", "HTTP Error ${e.code()}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("CryptoRepository", "Refresh failed: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getCoinPrice(coinId: String): Flow<CoinPrice?> {
        return coinDao.getCoinPriceFlow(coinId).map { it?.toDomainPrice() }
    }

    override suspend fun getOHLCData(coinId: String, days: Int): List<OHLCData> {
        return try {
            // Try CoinGecko first
            val response = api.getCoinOHLC(id = coinId, vsCurrency = "usd", days = days.toString())
            if (response.isNotEmpty()) {
                response.mapNotNull { item ->
                    if (item.size >= 5) {
                        OHLCData(
                            timestamp = item[0].toLong(),
                            open = item[1],
                            high = item[2],
                            low = item[3],
                            close = item[4],
                            volume = 0.0
                        )
                    } else null
                }
            } else {
                fetchOHLCFromBinance(coinId, days)
            }
        } catch (e: Exception) {
            Log.w("CryptoRepository", "CoinGecko OHLC failed for $coinId, trying Binance fallback...")
            fetchOHLCFromBinance(coinId, days)
        }
    }

    private suspend fun fetchOHLCFromBinance(coinId: String, days: Int): List<OHLCData> {
        return try {
            val symbol = when (coinId.lowercase()) {
                "bitcoin" -> "BTCUSDT"
                "ethereum" -> "ETHUSDT"
                "ripple" -> "XRPUSDT"
                "solana" -> "SOLUSDT"
                "cardano" -> "ADAUSDT"
                "polkadot" -> "DOTUSDT"
                "chainlink" -> "LINKUSDT"
                "litecoin" -> "LTCUSDT"
                "avalanche-2" -> "AVAXUSDT"
                "tron" -> "TRXUSDT"
                "matic-network" -> "MATICUSDT"
                "stellar" -> "XLMUSDT"
                "cosmos" -> "ATOMUSDT"
                else -> null
            } ?: return emptyList()

            val interval = when {
                days <= 2 -> "1h"
                days <= 30 -> "4h"
                else -> "1d"
            }

            val klines = binanceApi.getKlines(symbol, interval, limit = 100)
            klines.map { item ->
                OHLCData(
                    timestamp = (item[0] as Double).toLong(),
                    open = (item[1] as String).toDouble(),
                    high = (item[2] as String).toDouble(),
                    low = (item[3] as String).toDouble(),
                    close = (item[4] as String).toDouble(),
                    volume = (item[5] as String).toDouble()
                )
            }
        } catch (e: Exception) {
            Log.e("CryptoRepository", "Binance OHLC fallback failed for $coinId: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getCurrentPrice(coinId: String): Double {
        return try {
            val response = api.getSimplePrice(ids = coinId, vsCurrencies = "usd")
            val price = response[coinId]?.get("usd") ?: 0.0
            val aggPrice = aggregator.fetchAggregatedPrice(coinId, price)
            aggPrice.consensusPrice
        } catch (e: Exception) {
            getCachedPrice(coinId)
        }
    }

    override suspend fun getCachedPrice(coinId: String): Double {
        return coinDao.getCoinById(coinId)?.currentPrice ?: 0.0
    }

    override suspend fun getCachedChange24h(coinId: String): Double {
        return coinDao.getCoinById(coinId)?.priceChangePercentage24h ?: 0.0
    }

    override suspend fun getCoinDetail(coinId: String): Result<CoinDetail> {
        return try {
            val res = api.getCoinDetail(coinId)
            val detail = CoinDetail(
                id = res.id,
                symbol = res.symbol,
                name = res.name,
                description = res.description?.get("en") ?: "",
                homepage = res.links?.homepage?.firstOrNull() ?: "",
                currentPrice = res.marketData?.currentPrice?.get("usd") ?: 0.0,
                marketCap = res.marketData?.marketCap?.get("usd") ?: 0.0,
                totalVolume = res.marketData?.totalVolume?.get("usd") ?: 0.0,
                high24h = res.marketData?.high24h?.get("usd") ?: 0.0,
                low24h = res.marketData?.low24h?.get("usd") ?: 0.0,
                priceChangePercentage24h = res.marketData?.priceChangePercentage24h ?: 0.0,
                isTracked = coinDao.getCoinById(coinId)?.isTracked ?: false,
                sparkline = res.marketData?.sparkline7d?.price ?: emptyList(),
                markets = res.tickers?.map {
                    MarketTicker(
                        exchange = it.market.name,
                        pair = "${it.base}/${it.target}",
                        price = it.last,
                        volume = it.volume,
                        tradeUrl = it.tradeUrl ?: ""
                    )
                } ?: emptyList()
            )
            Result.success(detail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getGlobalMarketData(): Result<GlobalMarketData> {
        return try {
            val response = api.getGlobalData()
            val res = response.data
            val data = GlobalMarketData(
                activeCoins = res.activeCryptocurrencies,
                totalMarketCap = res.totalMarketCap["usd"] ?: 0.0,
                totalVolume = res.totalVolume["usd"] ?: 0.0,
                marketCapChangePercentage24h = res.marketCapChangePercentage24hUsd,
                btcDominance = res.marketCapPercentage["btc"] ?: 0.0,
                ethDominance = res.marketCapPercentage["eth"] ?: 0.0
            )
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPriceAtTimestamp(coinId: String, timestamp: Long): Double {
        return try {
            priceHistoryDao.getNearestPriceBeforeTimestamp(coinId, timestamp)
                ?.let { return it }
            // Fallback to current price if no history found
            getCurrentPrice(coinId)
        } catch (e: Exception) {
            Log.w("CryptoDept_Repo", "Failed to get price at timestamp for $coinId", e)
            getCurrentPrice(coinId)
        }
    }

    override suspend fun toggleTracking(coinId: String): Result<Unit> {
        return try {
            val coin = coinDao.getCoinById(coinId) ?: return Result.failure(Exception("COIN_NOT_FOUND"))
            
            if (!coin.isTracked) {
                // Trying to track
                val isPro = billingManager.isPro.value
                val count = coinDao.getTrackedCoinsCount()
                if (!isPro && count >= 3) {
                    return Result.failure(Exception("PRO_REQUIRED_LIMIT_3"))
                }
            }
            
            coinDao.updateCoin(coin.copy(isTracked = !coin.isTracked))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cleanup method for proper resource management.
     * Should be called when app is destroyed or repository is no longer needed.
     */
    fun cleanup() {
        stopPriceSubscriptions()
        repositoryScope.cancel()
        Log.d("CryptoDept_Repo", "Repository cleanup completed")
    }
}