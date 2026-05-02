package com.cryptodept.di

import android.content.Context
import com.cryptodept.data.api.*
import com.cryptodept.BuildConfig
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton
import java.io.File

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val COINGECKO_BASE_URL = "https://api.coingecko.com/api/v3/"
    private const val KRAKEN_BASE_URL = "https://api.kraken.com/0/public/"
    private const val COINBASE_BASE_URL = "https://api.coinbase.com/api/v3/brokerage/"
    private const val COINCAP_BASE_URL = "https://api.coincap.io/v2/"
    private const val COINPAPRIKA_BASE_URL = "https://api.coinpaprika.com/v1/"
    private const val FEAR_GREED_BASE_URL = "https://api.alternative.me/"
    private const val CRYPTOPANIC_BASE_URL = "https://cryptopanic.com/api/v1/"
    private const val CRYPTONEWS_BASE_URL = "https://cryptocurrency.cv/"
    private const val BLOCKCHAIN_BASE_URL = "https://api.blockchain.info/"
    private const val ETHERSCAN_BASE_URL = "https://api.etherscan.io/"

    private const val CACHE_SIZE_MB = 10L

    @Provides
    @Singleton
    fun provideHttpCache(
        @ApplicationContext context: Context
    ): Cache {
        val cacheDir = File(context.cacheDir, "http_cache")
        return Cache(cacheDir, CACHE_SIZE_MB * 1024 * 1024)
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
    }

    @Provides
    @Singleton
    fun provideRetryInterceptor(): RetryInterceptor {
        return RetryInterceptor(maxRetries = 3, initialDelayMs = 1000)
    }

    @Provides
    @Singleton
    @Named("PublicClient")
    fun providePublicOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        retryInterceptor: RetryInterceptor,
        cache: Cache
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(retryInterceptor)
            .addInterceptor(RateLimitInterceptor())
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=300")
                    .build()
            }
            .cache(cache)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    @Named("CoinGeckoClient")
    fun provideCoinGeckoOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        retryInterceptor: RetryInterceptor,
        cache: Cache
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(retryInterceptor)
            .addInterceptor(RateLimitInterceptor())
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-cg-demo-api-key", BuildConfig.COINGECKO_API_KEY)
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=600")  // 10 minutes cache for CoinGecko
                    .build()
            }
            .cache(cache)
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun createRetrofit(url: String, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides @Singleton
    fun provideCoinGeckoApi(@Named("CoinGeckoClient") client: OkHttpClient): CoinGeckoApi =
        createRetrofit(COINGECKO_BASE_URL, client).create(CoinGeckoApi::class.java)

    @Provides @Singleton
    fun provideKrakenApi(@Named("PublicClient") client: OkHttpClient): KrakenApi =
        createRetrofit(KRAKEN_BASE_URL, client).create(KrakenApi::class.java)

    @Provides @Singleton
    fun provideCoinbaseApi(@Named("PublicClient") client: OkHttpClient): CoinbaseApi =
        createRetrofit(COINBASE_BASE_URL, client).create(CoinbaseApi::class.java)

    @Provides @Singleton
    fun provideCoinCapApi(@Named("PublicClient") client: OkHttpClient): CoinCapApi =
        createRetrofit(COINCAP_BASE_URL, client).create(CoinCapApi::class.java)

    @Provides @Singleton
    fun provideCoinPaprikaApi(@Named("PublicClient") client: OkHttpClient): CoinPaprikaApi =
        createRetrofit(COINPAPRIKA_BASE_URL, client).create(CoinPaprikaApi::class.java)

    @Provides @Singleton
    fun provideFearGreedApi(@Named("PublicClient") client: OkHttpClient): FearGreedApi =
        createRetrofit(FEAR_GREED_BASE_URL, client).create(FearGreedApi::class.java)

    @Provides @Singleton
    fun provideNewsApiService(@Named("PublicClient") client: OkHttpClient): NewsApiService =
        createRetrofit(CRYPTOPANIC_BASE_URL, client).create(NewsApiService::class.java)

    @Provides @Singleton
    fun provideBlockchainApi(@Named("PublicClient") client: OkHttpClient): BlockchainApi =
        createRetrofit(BLOCKCHAIN_BASE_URL, client).create(BlockchainApi::class.java)

    @Provides @Singleton
    fun provideEtherscanApi(@Named("PublicClient") client: OkHttpClient): EtherscanApi =
        Retrofit.Builder()
            .baseUrl(ETHERSCAN_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EtherscanApi::class.java)

    @Provides @Singleton
    fun provideBinanceFuturesApi(@Named("binance_futures") retrofit: Retrofit): BinanceFuturesApi =
        retrofit.create(BinanceFuturesApi::class.java)

    @Provides @Singleton @Named("binance_futures")
    fun provideBinanceFuturesRetrofit(@Named("PublicClient") okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://fapi.binance.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton @Named("coinglass")
    fun provideCoinglassOkHttp(@Named("PublicClient") base: OkHttpClient): OkHttpClient =
        base.newBuilder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .addHeader("CG-API-KEY", BuildConfig.COINGLASS_API_KEY)
                        .build()
                )
            }
            .build()

    @Provides @Singleton @Named("coinglass")
    fun provideCoinglassRetrofit(@Named("coinglass") client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://open-api.coinglass.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideCoinglassApi(@Named("coinglass") retrofit: Retrofit): CoinglassApi =
        retrofit.create(CoinglassApi::class.java)

    @Provides @Singleton @Named("alphavantage")
    fun provideAlphaVantageRetrofit(@Named("PublicClient") okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://www.alphavantage.co/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideAlphaVantageApi(@Named("alphavantage") retrofit: Retrofit): AlphaVantageApi =
        retrofit.create(AlphaVantageApi::class.java)

    @Provides @Singleton @Named("coinmarketcal")
    fun provideCoinMarketCalRetrofit(@Named("PublicClient") okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://developers.coinmarketcal.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideCoinMarketCalApi(@Named("coinmarketcal") retrofit: Retrofit): CoinMarketCalApi =
        retrofit.create(CoinMarketCalApi::class.java)

    @Provides @Singleton
    fun provideGson(): Gson = Gson()
}
