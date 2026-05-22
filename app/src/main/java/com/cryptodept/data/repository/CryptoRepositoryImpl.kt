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
    private val firebaseDataSource: com.cryptodept.data.remote.source.FirebaseRemoteDataSource,
    private val subscription: com.cryptodept.data.datastore.SubscriptionAccessManager,
    private val demoMode: com.cryptodept.util.DemoModeProvider,
) : CryptoRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastFetchTime = 0L
    private val RATE_LIMIT_MS = 10_000L
    private var subscriptionJob: Job? = null
    private var cloudSyncJob: Job? = null

    @Inject
    lateinit var alertsRepository: AlertsRepository

    init {
        repositoryScope.launch {
            coinDao.deleteStablecoins()
        }
        startCloudSync()
    }

    private fun startCloudSync() {
        cloudSyncJob?.cancel()
        cloudSyncJob = repositoryScope.launch {
            // 1. Listen to Global State (Macro, Reports, Whale Alerts)
            firebaseDataSource.getTerminalState().collect { cloudState ->
                if (cloudState == null) return@collect

                // Sync Network Health / Macro Briefing
                cloudState.macroBriefing?.let { briefing ->
                    val health = NetworkHealth(
                        btcHashrate = "N/A",
                        btcMempool = "N/A",
                        ethGas = "${briefing.ethGasGwei} gwei",
                        fearGreedIndex = briefing.fearGreedIndex,
                        fearGreedLabel = when {
                            briefing.fearGreedIndex > 75 -> "Extreme Greed"
                            briefing.fearGreedIndex > 55 -> "Greed"
                            briefing.fearGreedIndex > 45 -> "Neutral"
                            briefing.fearGreedIndex > 25 -> "Fear"
                            else -> "Extreme Fear"
                        },
                        socialPulse = 50,
                        socialPulseLabel = "Neutral"
                    )
                    saveNetworkHealth(health)
                }
                
                // 2. Identify and Sync Top 5 Global Coins (Free for all)
                // We take top 5 from the cloud market data sorted by market cap
                val top5Ids = cloudState.marketData.values
                    .sortedByDescending { it.marketCap }
                    .take(5)
                    .map { it.id }
                    .toSet()
                
                syncSpecificCoins(top5Ids)
            }
        }
        
        // 3. Granular listeners for User Watchlist (3 for Free / 15 for Pro)
        repositoryScope.launch {
            coinDao.getTrackedCoins().collect { trackedEntities ->
                val limit = if (subscription.isPro.value) 15 else 3
                val trackedIds = trackedEntities.take(limit).map { it.id }.toSet()
                syncSpecificCoins(trackedIds)
            }
        }
    }

    private val activeSyncJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    private fun syncSpecificCoins(coinIds: Set<String>) {
        coinIds.forEach { id ->
            if (!activeSyncJobs.containsKey(id)) {
                val job = repositoryScope.launch {
                    firebaseDataSource.getCoinData(id).collect { cloudCoin ->
                        if (cloudCoin != null) {
                            updateLocalCoinFromCloud(cloudCoin)
                        }
                    }
                }
                activeSyncJobs[id] = job
            }
        }
        
        // Optional: Clean up jobs for coins no longer in top5 or watchlist
        // But since we want to keep them cached, we can leave them or add a timeout
    }

    private suspend fun updateLocalCoinFromCloud(cloudCoin: com.cryptodept.data.remote.model.CloudMarketData) {
        val localCoin = coinDao.getCoinById(cloudCoin.id)
        val entity = CoinEntity(
            id = cloudCoin.id,
            symbol = cloudCoin.symbol,
            name = localCoin?.name ?: cloudCoin.symbol.uppercase(),
            isTracked = localCoin?.isTracked ?: false,
            currentPrice = cloudCoin.currentPrice,
            priceChange24h = cloudCoin.priceChange24h,
            priceChangePercentage24h = (cloudCoin.priceChange24h / cloudCoin.currentPrice) * 100,
            marketCap = cloudCoin.marketCap,
            totalVolume = cloudCoin.volume24h,
            high24h = localCoin?.high24h ?: cloudCoin.currentPrice,
            low24h = localCoin?.low24h ?: cloudCoin.currentPrice,
            lastUpdated = System.currentTimeMillis()
        )
        coinDao.insertCoins(listOf(entity))
        alertsRepository.checkAlerts(cloudCoin.id, cloudCoin.currentPrice)
    }

    private val STABLECOIN_IDS = setOf(
        "tether", "usd-coin", "binance-usd", "dai", "true-usd", "paxos-standard", "frax", "usdd", "fdusd", "pyusd", "first-digital-usd", "paypal-usd", "ethena-usde", "usds", "tibbir", "figr_heloc"
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
        demoMode.demoActiveState.flatMapLatest { active ->
            if (active) {
                flowOf(demoMode.getDemoMarketsList().take(15).map { it.toDomain() })
            } else {
                coinDao.getTrackedCoins().flatMapLatest { tracked ->
                    if (tracked.isEmpty()) {
                        coinDao.getTopCoins(15).map { top -> top.map { it.toDomainPrice() } }
                    } else {
                        flowOf(tracked.map { it.toDomainPrice() })
                    }
                }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getAllCoinPrices(): Flow<List<CoinPrice>> = 
        demoMode.demoActiveState.flatMapLatest { active ->
            if (active) {
                flowOf(demoMode.getDemoMarketsList().map { it.toDomain() })
            } else {
                coinDao.getAllCoins().map { it.map { e -> e.toDomainPrice() } }
            }
        }

    override suspend fun refreshPrices(): CryptoResult<Unit> = coroutineScope {
        try {
            val now = System.currentTimeMillis()
            if (now - lastFetchTime < RATE_LIMIT_MS) return@coroutineScope CryptoResult.Success(Unit)
            
            // PHASE C: Check Cloud Freshness
            val cloud = firebaseDataSource.getTerminalState().firstOrNull()
            if (cloud != null && (now - cloud.lastUpdateTimestamp) < 300_000) { // 5 mins
                // Cloud is fresh, already synced via startCloudSync(), skipping expensive CG call
                lastFetchTime = now
                return@coroutineScope CryptoResult.Success(Unit)
            }

            val response = runCatching { 
                withTimeout(10000) { api.getCoinMarkets(perPage = 100) }
            }
            val marketResponse = response.getOrNull()
            
            if (marketResponse == null || marketResponse.isEmpty()) {
                // Fallback: update only base coins from aggregator
                val baseIds = listOf("bitcoin", "ethereum", "ripple", "solana", "binancecoin", "dogecoin")
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

    override fun getGlobalMarketDataFlow(): Flow<GlobalMarketData?> = 
        firebaseDataSource.getTerminalState().map { state ->
            state?.macroBriefing?.let { b ->
                GlobalMarketData(
                    activeCoins = 10000, // Placeholder
                    totalMarketCap = b.globalMarketCapUsd,
                    totalVolume = 0.0, // Placeholder
                    marketCapChangePercentage24h = 0.0,
                    btcDominance = b.btcDominance,
                    ethDominance = 15.0 // Placeholder
                )
            }
        }

    override suspend fun getPriceAtTimestamp(coinId: String, timestamp: Long): Double =
        priceHistoryRepository.getPriceAtTimestamp(coinId, timestamp) ?: getCurrentPrice(coinId)

    override suspend fun toggleTracking(coinId: String): CryptoResult<Unit> = try {
        val coin = coinDao.getCoinById(coinId) ?: throw Exception("NOT_FOUND")
        val currentTrackedCount = coinDao.getTrackedCoinsCount()
        val isPro = subscription.isPro.value
        
        if (!coin.isTracked) {
            val limit = if (isPro) 15 else 3 // STRICT LIMIT: 3 for Free, 15 for Pro
            if (currentTrackedCount >= limit) {
                throw Exception("LIMIT_REACHED: MAX_${limit}_COINS")
            }
        }

        coinDao.updateCoin(coin.copy(isTracked = !coin.isTracked))
        CryptoResult.Success(Unit)
    } catch (e: Exception) {
        CryptoResult.Error(e)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getNetworkHealth(): Flow<NetworkHealth?> = 
        demoMode.demoActiveState.flatMapLatest { active ->
            if (active) {
                val d = demoMode.getDemoNetworkHealth()
                val s = demoMode.getDemoSentiment()
                flowOf(NetworkHealth(
                    btcHashrate = "${d.btcGasFeeSat} sat",
                    btcMempool = "${d.mempoolBacklog} txs",
                    ethGas = "${d.ethGasFeeGwei} gwei",
                    fearGreedIndex = s.fearGreedIndex,
                    fearGreedLabel = s.fearGreedLabel,
                    socialPulse = s.redditPositive,
                    socialPulseLabel = if (s.redditPositive > 60) "Bullish" else "Neutral"
                ))
            } else {
                networkHealthDao.getNetworkHealth().map { it?.toDomain() }
            }
        }

    private fun com.cryptodept.util.DemoMarketCoin.toDomain() = CoinPrice(
        id = symbol.lowercase(),
        symbol = symbol,
        name = name,
        currentPrice = price,
        priceChange24h = (change24h / 100) * price,
        priceChangePercentage24h = change24h,
        marketCap = marketCap.toDouble(),
        totalVolume = 100_000_000.0,
        high24h = price * 1.05,
        low24h = price * 0.95,
        lastUpdated = System.currentTimeMillis(),
        isTracked = true
    )

    override suspend fun saveNetworkHealth(health: NetworkHealth) {
        networkHealthDao.insertNetworkHealth(com.cryptodept.data.db.NetworkHealthEntity.fromDomain(health))
    }

    fun cleanup() {
        stopPriceSubscriptions()
        repositoryScope.cancel()
    }
}
