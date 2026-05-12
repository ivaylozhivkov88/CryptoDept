package com.cryptodept.data.repository

import com.cryptodept.data.api.*
import com.cryptodept.data.db.CoinDao
import com.cryptodept.data.db.CoinEntity
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoRepositoryImpl @Inject constructor(
    private val api: CoinGeckoApi,
    private val cgSource: CoinGeckoPriceSource,
    private val binanceSource: BinancePriceSource,
    private val aggregator: MultiSourcePriceAggregator,
    private val coinDao: CoinDao,
    private val priceHistoryRepository: PriceHistoryRepository,
    private val networkHealthDao: com.cryptodept.data.db.NetworkHealthDao,
    private val binanceWS: BinanceWebSocketService,
    private val krakenWS: KrakenWebSocketService,
) : CryptoRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastFetchTime = 0L
    private val RATE_LIMIT_MS = 10_000L
    private var subscriptionJob: Job? = null

    @Inject
    lateinit var alertsRepository: AlertsRepository

    init {
        repositoryScope.launch {
            coinDao.deleteStablecoins()
        }
    }

    private val STABLECOIN_IDS = setOf(
        "tether", "usd-coin", "binance-usd", "dai", "true-usd", "paxos-standard", "frax", "usdd", "fdusd", "pyusd", "first-digital-usd", "paypal-usd", "ethena-usde"
    )

    internal fun startPriceSubscriptions() {
        if (subscriptionJob?.isActive == true) return
        subscriptionJob = repositoryScope.launch {
            launch {
                binanceWS.observeTickerStream().collect { ticker ->
                    val coinId = when (ticker.symbol) {
                        "BTCUSDT" -> "bitcoin"
                        "ETHUSDT" -> "ethereum"
                        else -> null
                    }
                    coinId?.let { updateConsensusPrice(it, "binance", ticker.lastPrice.toDouble()) }
                }
            }
            launch {
                krakenWS.observeTickerStream().collect { (coinId, price) ->
                    updateConsensusPrice(coinId, "kraken", price)
                }
            }
        }
    }

    internal fun stopPriceSubscriptions() {
        subscriptionJob?.cancel()
        subscriptionJob = null
    }

    private suspend fun updateConsensusPrice(coinId: String, source: String, price: Double) {
        try {
            val aggPrice = aggregator.updatePriceFromWS(coinId, source, price)
            coinDao.updatePrice(
                id = coinId,
                newPrice = aggPrice.consensusPrice,
                sourcesCount = aggPrice.sourcesCount,
                deviation = aggPrice.maxDeviationPercent,
                timestamp = System.currentTimeMillis()
            )
            alertsRepository.checkAlerts(coinId, aggPrice.consensusPrice)
        } catch (_: Exception) {}
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTrackedCoinPrices(): Flow<List<CoinPrice>> = 
        coinDao.getTrackedCoins().flatMapLatest { tracked ->
            if (tracked.isEmpty()) {
                coinDao.getTopCoins(15).map { top -> top.map { it.toDomainPrice() } }
            } else {
                flowOf(tracked.map { it.toDomainPrice() })
            }
        }

    override fun getAllCoinPrices(): Flow<List<CoinPrice>> = 
        coinDao.getAllCoins().map { it.map { e -> e.toDomainPrice() } }

    override suspend fun refreshPrices(): CryptoResult<Unit> = coroutineScope {
        try {
            if (System.currentTimeMillis() - lastFetchTime < RATE_LIMIT_MS) return@coroutineScope CryptoResult.Success(Unit)
            
            val response = runCatching { 
                withTimeout(10000) { api.getCoinMarkets(perPage = 15) }
            }
            val marketResponse = response.getOrNull()
            
            if (marketResponse == null || marketResponse.isEmpty()) {
                // Fallback: update only base coins from aggregator
                val baseIds = listOf("bitcoin", "ethereum", "ripple", "solana", "cardano", "dogecoin")
                baseIds.forEach { coinId ->
                    try {
                        val agg = aggregator.fetchAggregatedPrice(coinId, null)
                        coinDao.updatePrice(coinId, agg.consensusPrice, agg.sourcesCount, agg.maxDeviationPercent, System.currentTimeMillis())
                    } catch (_: Exception) {}
                }
                lastFetchTime = System.currentTimeMillis()
                return@coroutineScope CryptoResult.Success(Unit)
            }

            lastFetchTime = System.currentTimeMillis()

            val entities = marketResponse
                .filter { !STABLECOIN_IDS.contains(it.id) }
                .map { res ->
                    CoinEntity(
                    id = res.id,
                    symbol = res.symbol,
                    name = res.name,
                    isTracked = coinDao.getCoinById(res.id)?.isTracked ?: (res.market_cap_rank <= 10),
                    currentPrice = res.current_price,
                    priceChange24h = res.price_change_24h,
                    priceChangePercentage24h = res.price_change_percentage_24h,
                    marketCap = res.market_cap,
                    totalVolume = res.total_volume,
                    high24h = res.high_24h,
                    low24h = res.low_24h,
                    lastUpdated = System.currentTimeMillis()
                )
            }
            coinDao.insertCoins(entities)
            
            // Save to price history (one record per day logic inside repo)
            entities.forEach { coin ->
                priceHistoryRepository.saveDailyPrice(coin.id, coin.currentPrice, coin.totalVolume)
            }

            CryptoResult.Success(Unit)
        } catch (e: Exception) {
            CryptoResult.Error(e)
        }
    }

    override fun getCoinPrice(coinId: String): Flow<CoinPrice?> = 
        coinDao.getCoinPriceFlow(coinId).map { it?.toDomainPrice() }

    override suspend fun getOHLCData(coinId: String, days: Int): List<OHLCData> =
        try {
            cgSource.getOHLCData(coinId, days).ifEmpty { 
                binanceSource.getOHLCData(coinId, days) 
            }
        } catch (e: Exception) {
            binanceSource.getOHLCData(coinId, days)
        }

    override suspend fun getCurrentPrice(coinId: String): Double = 
        cgSource.getCurrentPrice(coinId) ?: getCachedPrice(coinId)

    override suspend fun getCachedPrice(coinId: String): Double = 
        coinDao.getCoinById(coinId)?.currentPrice ?: 0.0

    override suspend fun getCachedChange24h(coinId: String): Double = 
        coinDao.getCoinById(coinId)?.priceChangePercentage24h ?: 0.0

    override suspend fun getCoinDetail(coinId: String): CryptoResult<CoinDetail> = try {
        val res = api.getCoinDetail(coinId)
        CryptoResult.Success(CoinDetail(
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
            markets = res.tickers?.map { ticker ->
                MarketTicker(
                    exchange = ticker.market?.name ?: "Unknown",
                    pair = "${ticker.base}/${ticker.target}",
                    price = ticker.last ?: 0.0,
                    volume = ticker.volume ?: 0.0,
                    tradeUrl = ticker.tradeUrl
                )
            } ?: emptyList()
        ))
    } catch (e: Exception) {
        CryptoResult.Error(e)
    }

    override suspend fun getGlobalMarketData(): CryptoResult<GlobalMarketData> = try {
        val res = api.getGlobalData().data
        CryptoResult.Success(GlobalMarketData(
            activeCoins = res.activeCryptocurrencies,
            totalMarketCap = res.totalMarketCap["usd"] ?: 0.0,
            totalVolume = res.totalVolume["usd"] ?: 0.0,
            marketCapChangePercentage24h = res.marketCapChangePercentage24hUsd,
            btcDominance = res.marketCapPercentage["btc"] ?: 0.0,
            ethDominance = res.marketCapPercentage["eth"] ?: 0.0
        ))
    } catch (e: Exception) {
        CryptoResult.Error(e)
    }

    override suspend fun getPriceAtTimestamp(coinId: String, timestamp: Long): Double =
        priceHistoryRepository.getPriceAtTimestamp(coinId, timestamp) ?: getCurrentPrice(coinId)

    override suspend fun toggleTracking(coinId: String): CryptoResult<Unit> = try {
        val coin = coinDao.getCoinById(coinId) ?: throw Exception("NOT_FOUND")
        coinDao.updateCoin(coin.copy(isTracked = !coin.isTracked))
        CryptoResult.Success(Unit)
    } catch (e: Exception) {
        CryptoResult.Error(e)
    }

    override fun getNetworkHealth(): Flow<NetworkHealth?> = 
        networkHealthDao.getNetworkHealth().map { it?.toDomain() }

    override suspend fun saveNetworkHealth(health: NetworkHealth) {
        networkHealthDao.insertNetworkHealth(com.cryptodept.data.db.NetworkHealthEntity.fromDomain(health))
    }

    fun cleanup() {
        stopPriceSubscriptions()
        repositoryScope.cancel()
    }
}
