package com.cryptodept.data.repository

import android.util.Log
import com.cryptodept.data.api.CoinGeckoApi
import com.cryptodept.data.api.MultiSourcePriceAggregator
import com.cryptodept.data.api.BinanceWebSocketManager
import com.cryptodept.data.api.KrakenWebSocketManager
import com.cryptodept.data.db.CoinDao
import com.cryptodept.data.db.CoinEntity
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.CryptoRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoRepositoryImpl @Inject constructor(
    private val api: CoinGeckoApi,
    private val aggregator: MultiSourcePriceAggregator,
    private val coinDao: CoinDao,
    private val binanceWS: BinanceWebSocketManager,
    private val krakenWS: KrakenWebSocketManager
) : CryptoRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastFetchTime = 0L
    private val RATE_LIMIT_MS = 10_000L

    init {
        binanceWS.connect()
        startPriceSubscriptions()
    }

    private fun startPriceSubscriptions() {
        // Binance WS
        repositoryScope.launch {
            binanceWS.observeTickerStream().collect { ticker ->
                val coinId = when(ticker.symbol) {
                    "BTCUSDT" -> "bitcoin"
                    "ETHUSDT" -> "ethereum"
                    "XRPUSDT" -> "ripple"
                    else -> null
                }
                coinId?.let { 
                    Log.d("CryptoDept_WS", "📡 Price update: $it = $${ticker.lastPrice}")
                    updateConsensusPrice(it, "binance", ticker.lastPrice.toDouble()) 
                }
            }
        }

        // Kraken WS
        repositoryScope.launch {
            krakenWS.observeTickerStream().collect { (coinId, price) ->
                updateConsensusPrice(coinId, "kraken", price)
            }
        }
    }

    private suspend fun updateConsensusPrice(coinId: String, source: String, price: Double) {
        val aggPrice = aggregator.updatePriceFromWS(coinId, source, price)
        coinDao.updatePrice(
            id = coinId,
            newPrice = aggPrice.consensusPrice,
            sourcesCount = aggPrice.sourcesCount,
            deviation = aggPrice.maxDeviationPercent,
            timestamp = System.currentTimeMillis()
        )
    }

    override fun getTrackedCoinPrices(): Flow<List<CoinPrice>> {
        return coinDao.getTrackedCoins().map { entities ->
            Log.d("CryptoDept_CACHE", "💾 Cache hit: ${entities.size} coins")
            entities.map { it.toDomainPrice() }
        }
    }

    override suspend fun refreshPrices(): Result<Unit> = coroutineScope {
        try {
            if (System.currentTimeMillis() - lastFetchTime < RATE_LIMIT_MS) {
                return@coroutineScope Result.success(Unit)
            }

            val trackedCoins = listOf(
                "bitcoin", "ethereum", "ripple", "cardano", "solana", 
                "polkadot", "dogecoin", "chainlink", "shiba-inu", "litecoin",
                "avalanche-2", "tron", "matic-network", "stellar", "cosmos"
            )
            val ids = trackedCoins.joinToString(",")
            
            Log.d("CryptoDept_API", "🌐 Fetching CoinGecko prices...")
            val marketResponse = api.getCoinMarkets(ids = ids)
            lastFetchTime = System.currentTimeMillis()
            Log.d("CryptoDept_API", "✅ CoinGecko prices loaded: ${marketResponse.size} coins")
            
            val entities = marketResponse.map { res ->
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
            Log.e("CryptoDept_API", "❌ CoinGecko error ${e.code()}: ${e.message()}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("CryptoDept_API", "❌ CoinGecko unknown error: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getCoinPrice(coinId: String): Flow<CoinPrice?> {
        return coinDao.getTrackedCoins().map { entities ->
            entities.find { it.id == coinId }?.toDomainPrice()
        }
    }

    override suspend fun getOHLCData(coinId: String, days: Int): List<com.cryptodept.domain.model.OHLCData> {
        return try {
            val response = api.getCoinOHLC(coinId, "usd", days.toString())
            response.map {
                com.cryptodept.domain.model.OHLCData(
                    timestamp = it[0].toLong(),
                    open = it[1],
                    high = it[2],
                    low = it[3],
                    close = it[4],
                    volume = 0.0 // CoinGecko OHLC endpoint doesn't return volume
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getCachedPrice(coinId: String): Double {
        return coinDao.getCoinById(coinId)?.currentPrice ?: 0.0
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
                sparkline = res.marketData?.sparkline7d?.price ?: emptyList(),
                markets = res.tickers?.map {
                    MarketTicker(
                        exchange = it.market.name,
                        pair = "${it.base}/${it.target}",
                        price = it.last,
                        volume = it.volume,
                        tradeUrl = it.tradeUrl
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
            val res = api.getGlobalData().data
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
}
