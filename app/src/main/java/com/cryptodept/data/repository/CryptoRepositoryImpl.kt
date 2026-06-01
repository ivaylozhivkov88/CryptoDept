package com.cryptodept.data.repository

import com.cryptodept.data.api.*
import com.cryptodept.data.db.CoinDao
import com.cryptodept.data.db.CoinEntity
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
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
    private val auth: com.google.firebase.auth.FirebaseAuth,
) : CryptoRepository {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastFetchTime = 0L
    private val RATE_LIMIT_MS = 10_000L
    private var subscriptionJob: Job? = null
    private var cloudSyncJob: Job? = null
    private var preFetchJob: Job? = null
    private var watchlistSyncJob: Job? = null
    private val activeSyncJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

    @Inject
    lateinit var alertsRepository: AlertsRepository

    init {
        repositoryScope.launch {
            coinDao.deleteStablecoins()
        }
        startCloudSync()
        startBackgroundPreFetch()
        observeAuthChanges()
    }

    private fun observeAuthChanges() {
        repositoryScope.launch {
            // Listen for UID changes (login/logout)
            val uidFlow = callbackFlow {
                val listener = com.google.firebase.auth.FirebaseAuth.AuthStateListener { firebaseAuth ->
                    trySend(firebaseAuth.currentUser?.uid)
                }
                auth.addAuthStateListener(listener)
                awaitClose { auth.removeAuthStateListener(listener) }
            }

            uidFlow.distinctUntilChanged().collect { uid ->
                watchlistSyncJob?.cancel()
                if (uid != null) {
                    // Small delay to ensure Firebase Auth tokens are settled before first write
                    delay(2000)
                    startWatchlistCloudSync(uid)
                }
            }
        }
    }

    private fun startWatchlistCloudSync(uid: String) {
        watchlistSyncJob = repositoryScope.launch {
            firebaseDataSource.getUserWatchlist(uid).collect { cloudIds ->
                if (cloudIds.isEmpty()) return@collect
                
                val currentTracked = coinDao.getTrackedCoins().first()
                val localIds = currentTracked.map { it.id }.toSet()
                val cloudSet = cloudIds.toSet()

                // A. PULL: If it's in Cloud but NOT in Local -> Add it
                val missingIds = cloudSet.filter { !localIds.contains(it) }
                if (missingIds.isNotEmpty()) {
                    try {
                        // Batch fetch missing coins from API to populate local DB
                        val response = api.getCoinMarkets(ids = missingIds.joinToString(","), perPage = 100)
                        val newEntities = response.map { res ->
                            CoinEntity(
                                id = res.id,
                                symbol = res.symbol,
                                name = res.name,
                                isTracked = true,
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
                        if (newEntities.isNotEmpty()) {
                            coinDao.insertCoins(newEntities)
                        }
                        
                        // Fallback: If some IDs weren't found in markets call (e.g. niche coins), 
                        // try to mark them as tracked if they already exist in DB but aren't tracked
                        missingIds.forEach { id ->
                            coinDao.getCoinById(id)?.let { 
                                if (!it.isTracked) coinDao.updateCoin(it.copy(isTracked = true))
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Sync", "Batch pull failed: ${e.message}")
                    }
                }

                // B. PUSH: Ensure cloud has everything local has
                currentTracked.forEach { coin ->
                    if (!cloudSet.contains(coin.id)) {
                        launch { firebaseDataSource.setUserWatchlist(uid, coin.id, true) }
                    }
                }
            }
        }
    }

    private fun startBackgroundPreFetch() {
        preFetchJob?.cancel()
        preFetchJob = repositoryScope.launch {
            // Wait for initial boot to settle
            delay(10000)
            
            // Get Top 20 coins to ensure we have cached history for the most important assets
            val topCoins = coinDao.getTopCoins(20).first()
            
            topCoins.forEach { coin ->
                val now = System.currentTimeMillis()
                val lastCache = coin.lastUpdated
                
                // If cache is older than 24 hours (Sync once per day as requested)
                if (now - lastCache > 86_400_000) {
                    try {
                        val data = cgSource.getOHLCData(coin.id, 7)
                        if (data.isNotEmpty()) {
                            priceHistoryRepository.saveOHLCData(coin.id, data)
                            // Minimal update to coin entity to track last sync
                            coinDao.updatePrice(coin.id, coin.currentPrice, 0, 0.0, now)
                        }
                    } catch (_: Exception) {}
                    
                    // CRITICAL: Delay between coins to avoid rate limits (20 seconds)
                    delay(20_000)
                }
            }
        }
    }

    private fun startCloudSync() {
        cloudSyncJob?.cancel()
        cloudSyncJob = repositoryScope.launch {
            // 1. Listen ONLY to Macro Data (Small payload, massive traffic saving)
            launch {
                firebaseDataSource.getGlobalState().collect { briefing ->
                    if (briefing == null) return@collect
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
            }

            // 2. Identify and Sync Top 5 Global Coins + Watchlist
            // We use hardcoded top 5 to avoid listening to the whole marketData map
            val baseTop5 = setOf("bitcoin", "ethereum", "binancecoin", "solana", "ripple")
            syncSpecificCoins(baseTop5)
        }
        
        // 3. Watchlist Listeners (Limit 3 for Free / 30 for Pro)
        repositoryScope.launch {
            coinDao.getTrackedCoins().collect { trackedEntities ->
                val limit = if (subscription.isPro.value) 30 else 3
                val trackedIds = trackedEntities.take(limit).map { it.id }.toSet()
                
                // Stop listeners for removed coins
                activeSyncJobs.keys.forEach { activeId ->
                    if (!trackedIds.contains(activeId) && !setOf("bitcoin", "ethereum", "binancecoin", "solana", "ripple").contains(activeId)) {
                        activeSyncJobs[activeId]?.cancel()
                        activeSyncJobs.remove(activeId)
                    }
                }

                syncSpecificCoins(trackedIds)
            }
        }
    }

    private fun syncSpecificCoins(coinIds: Set<String>) {
        coinIds.forEach { id ->
            if (activeSyncJobs != null && !activeSyncJobs.containsKey(id)) {
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
            val dbCount = coinDao.getCoinsCount()
            
            // PHASE C: Check Cloud Freshness (Lightweight call)
            // CRITICAL FIX: If DB is empty, IGNORE cloud freshness and force a fetch to populate the UI.
            if (dbCount > 0) {
                val cloudLastUpdate = firebaseDataSource.getLastUpdateTimestamp()
                if (cloudLastUpdate > 0 && (now - cloudLastUpdate) < 600_000) { // 10 mins
                    // Cloud is fresh, already synced via startCloudSync(), skipping expensive CG call
                    lastFetchTime = now
                    return@coroutineScope CryptoResult.Success(Unit)
                }
                
                if (now - lastFetchTime < RATE_LIMIT_MS) return@coroutineScope CryptoResult.Success(Unit)
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
                    isTracked = coinDao.getCoinById(res.id)?.isTracked ?: false,
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

    override suspend fun getOHLCData(coinId: String, days: Int): List<OHLCData> {
        // 1. Check Cloud-Cached Data First (Firebase)
        try {
            val cloudPrediction = firebaseDataSource.getCloudPrediction(coinId)
            // Look for 30d packet first, then 14d fallback
            val cloudOhlcRaw = (cloudPrediction?.get("ohlc30d") ?: cloudPrediction?.get("ohlc14d")) as? List<*>
            
            if (cloudOhlcRaw != null) {
                val cloudData = cloudOhlcRaw.mapNotNull { item ->
                    val list = item as? List<*>
                    if (list != null && list.size >= 5) {
                        OHLCData(
                            timestamp = (list[0] as Number).toLong(),
                            open = (list[1] as Number).toDouble(),
                            high = (list[2] as Number).toDouble(),
                            low = (list[3] as Number).toDouble(),
                            close = (list[4] as Number).toDouble(),
                            volume = 0.0
                        )
                    } else null
                }
                if (cloudData.isNotEmpty() && cloudData.size >= (days * 0.8).toInt()) {
                    priceHistoryRepository.saveOHLCData(coinId, cloudData)
                    return cloudData
                }
            }
        } catch (_: Exception) {}

        // 2. Check Local Cache (Room)
        val cached = priceHistoryRepository.getOHLCData(coinId, days)
        val now = System.currentTimeMillis()
        
        if (cached.size >= (days * 0.9).toInt()) {
            val lastDataPoint = cached.last().timestamp
            // If the latest data point is newer than 4 hours, it's fresh enough for daily analysis
            if (now - lastDataPoint < 14_400_000) {
                return cached
            }
        }

        // 3. Fetch Fresh Data if cloud and local cache missing or old
        val freshData = try {
            val cgData = cgSource.getOHLCData(coinId, days)
            if (cgData.isNotEmpty()) cgData else {
                val binanceData = binanceSource.getOHLCData(coinId, days)
                binanceData
            }
        } catch (e: Exception) {
            binanceSource.getOHLCData(coinId, days)
        }

        // 4. Update Cache and return
        if (freshData.isNotEmpty()) {
            priceHistoryRepository.saveOHLCData(coinId, freshData)
            return freshData
        }

        // 5. FINAL FALLBACK: If fresh fetch failed (Rate Limit), return cached regardless of age
        // instead of returning empty list which crashes analysis
        if (cached.isNotEmpty()) return cached

        // If even cache is empty, we return a tiny synthetic trend based on current price to avoid the RED CRITICAL ERROR
        val currentPrice = runBlocking { getCachedPrice(coinId).coerceAtLeast(0.01) }
        val nowTs = System.currentTimeMillis()
        return List(days) { i ->
            OHLCData(nowTs - (days - i) * 86400000L, currentPrice, currentPrice, currentPrice, currentPrice, 1.0)
        }
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
        firebaseDataSource.getGlobalState().map { briefing ->
            briefing?.let { b ->
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
        var coin = coinDao.getCoinById(coinId)
        
        // If coin not in DB (e.g. from search), fetch basic info and insert it
        if (coin == null) {
            val searchResults = searchCoins(coinId)
            val found = searchResults.find { it.id == coinId }
            if (found != null) {
                val entity = CoinEntity(
                    id = found.id,
                    symbol = found.symbol,
                    name = found.name,
                    isTracked = false,
                    currentPrice = found.currentPrice,
                    priceChange24h = 0.0,
                    priceChangePercentage24h = 0.0,
                    marketCap = 0.0,
                    totalVolume = 0.0,
                    high24h = 0.0,
                    low24h = 0.0,
                    lastUpdated = System.currentTimeMillis()
                )
                coinDao.insertCoins(listOf(entity))
                coin = entity
            } else {
                throw Exception("NOT_FOUND")
            }
        }

        val currentTrackedCount = coinDao.getTrackedCoinsCount()
        val isPro = subscription.isPro.value
        
        if (!coin.isTracked) {
            val limit = if (isPro) 30 else 3 // STRICT LIMIT: 3 for Free, 30 for Pro
            if (currentTrackedCount >= limit) {
                throw Exception("LIMIT_REACHED: MAX_${limit}_COINS")
            }
        }

        val isNewTracked = !coin.isTracked
        coinDao.updateCoin(coin.copy(isTracked = isNewTracked))
        
        // Sync to cloud if logged in
        auth.currentUser?.uid?.let { uid ->
            repositoryScope.launch {
                firebaseDataSource.setUserWatchlist(uid, coinId, isNewTracked)
            }
        }

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

    override suspend fun searchCoins(query: String): List<CoinPrice> = withContext(Dispatchers.IO) {
        try {
            // First check local DB
            val local = coinDao.getAllCoins().first().filter { 
                it.symbol.contains(query, true) || it.name.contains(query, true)
            }.map { it.toDomainPrice() }
            
            if (local.size >= 5) return@withContext local
            
            // If few results, fetch from API (CoinGecko search)
            val apiResults = api.searchCoins(query).coins.take(10).map { res ->
                CoinPrice(
                    id = res.id,
                    symbol = res.symbol,
                    name = res.name,
                    currentPrice = 0.0,
                    priceChange24h = 0.0,
                    priceChangePercentage24h = 0.0,
                    marketCap = 0.0,
                    totalVolume = 0.0,
                    high24h = 0.0,
                    low24h = 0.0,
                    lastUpdated = System.currentTimeMillis(),
                    isTracked = coinDao.getCoinById(res.id)?.isTracked ?: false
                )
            }
            (local + apiResults).distinctBy { it.id }
        } catch (e: Exception) {
            emptyList()
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
