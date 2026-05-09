package com.cryptodept.di

import android.content.Context
import com.cryptodept.BuildConfig
import com.cryptodept.data.api.*
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    private const val CACHE_SIZE_MB = 10L

    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner =
        CertificatePinner.DEFAULT


    @Provides
    @Singleton
    fun provideHttpCache(
        @ApplicationContext context: Context,
    ): Cache {
        val cacheDir = File(context.cacheDir, "http_cache")
        return Cache(cacheDir, CACHE_SIZE_MB * 1024 * 1024)
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

    @Provides
    @Singleton
    fun provideRetryInterceptor(): RetryInterceptor = RetryInterceptor(maxRetries = 3, initialDelayMs = 1000)

    @Provides
    @Singleton
    fun provideFirebaseAnalyticsInterceptor(analyticsService: com.cryptodept.util.AnalyticsService): FirebaseAnalyticsInterceptor =
        FirebaseAnalyticsInterceptor(analyticsService)

    @Provides
    @Singleton
    @Named("PublicClient")
    fun providePublicOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        retryInterceptor: RetryInterceptor,
        authInterceptor: AuthInterceptor,
        firebaseInterceptor: FirebaseAnalyticsInterceptor,
        certificatePinner: CertificatePinner,
        cache: Cache,
    ): OkHttpClient {
        val builder =
            OkHttpClient
                .Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(retryInterceptor)
                .addInterceptor(authInterceptor)
                .addInterceptor(firebaseInterceptor)
                .addInterceptor(RateLimitInterceptor())
                .addNetworkInterceptor { chain ->
                    val response = chain.proceed(chain.request())
                    response
                        .newBuilder()
                        .header("Cache-Control", "public, max-age=300")
                        .build()
                }.cache(cache)
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)

        if (!BuildConfig.DEBUG) {
            builder.certificatePinner(certificatePinner)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    @Named("CoinGeckoClient")
    fun provideCoinGeckoOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        retryInterceptor: RetryInterceptor,
        authInterceptor: AuthInterceptor,
        firebaseInterceptor: FirebaseAnalyticsInterceptor,
        certificatePinner: CertificatePinner,
        cache: Cache,
    ): OkHttpClient {
        val builder =
            OkHttpClient
                .Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(retryInterceptor)
                .addInterceptor(authInterceptor)
                .addInterceptor(firebaseInterceptor)
                .addInterceptor(RateLimitInterceptor())
                .addInterceptor { chain ->
                    val request =
                        chain
                            .request()
                            .newBuilder()
                            .addHeader("x-cg-demo-api-key", BuildConfig.COINGECKO_API_KEY)
                            .addHeader("Accept", "application/json")
                            .build()
                    chain.proceed(request)
                }.addNetworkInterceptor { chain ->
                    val response = chain.proceed(chain.request())
                    response
                        .newBuilder()
                        .header("Cache-Control", "public, max-age=600")
                        .build()
                }.cache(cache)
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)

        if (!BuildConfig.DEBUG) {
            builder.certificatePinner(certificatePinner)
        }

        return builder.build()
    }

    private fun createRetrofit(
        url: String,
        client: OkHttpClient,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideCoinGeckoApi(
        @Named("CoinGeckoClient") client: OkHttpClient,
        config: EndpointsConfig,
    ): CoinGeckoApi = createRetrofit(config.coinGeckoBaseUrl, client).create(CoinGeckoApi::class.java)

    @Provides @Singleton
    fun provideKrakenApi(
        @Named("PublicClient") client: OkHttpClient,
        config: EndpointsConfig,
    ): KrakenApi = createRetrofit(config.krakenBaseUrl, client).create(KrakenApi::class.java)

    @Provides @Singleton
    fun provideCoinbaseApi(
        @Named("PublicClient") client: OkHttpClient,
        config: EndpointsConfig,
    ): CoinbaseApi = createRetrofit(config.coinbaseBaseUrl, client).create(CoinbaseApi::class.java)

    @Provides @Singleton
    fun provideCoinCapApi(
        @Named("PublicClient") client: OkHttpClient,
        config: EndpointsConfig,
    ): CoinCapApi = createRetrofit(config.coinCapBaseUrl, client).create(CoinCapApi::class.java)

    @Provides @Singleton
    fun provideCoinPaprikaApi(
        @Named("PublicClient") client: OkHttpClient,
        config: EndpointsConfig,
    ): CoinPaprikaApi = createRetrofit(config.coinPaprikaBaseUrl, client).create(CoinPaprikaApi::class.java)

    @Provides @Singleton
    fun provideFearGreedApi(
        @Named("PublicClient") client: OkHttpClient,
        config: EndpointsConfig,
    ): FearGreedApi = createRetrofit(config.fearGreedBaseUrl, client).create(FearGreedApi::class.java)

    @Provides @Singleton
    fun provideNewsApiService(
        @Named("PublicClient") client: OkHttpClient,
        config: EndpointsConfig,
    ): NewsApiService = createRetrofit(config.cryptoPanicBaseUrl, client).create(NewsApiService::class.java)

    @Provides @Singleton
    fun provideBlockchainApi(
        @Named("PublicClient") client: OkHttpClient,
        config: EndpointsConfig,
    ): BlockchainApi = createRetrofit(config.blockchainBaseUrl, client).create(BlockchainApi::class.java)

    @Provides @Singleton
    fun provideEtherscanApi(
        @Named("PublicClient") client: OkHttpClient,
        config: EndpointsConfig,
    ): EtherscanApi = createRetrofit(config.etherscanBaseUrl, client).create(EtherscanApi::class.java)

    @Provides @Singleton
    fun provideBinanceFuturesApi(
        @Named("binance_futures") retrofit: Retrofit,
    ): BinanceFuturesApi = retrofit.create(BinanceFuturesApi::class.java)

    @Provides @Singleton
    @Named("binance_futures")
    fun provideBinanceFuturesRetrofit(
        @Named("PublicClient") okHttpClient: OkHttpClient,
        config: EndpointsConfig,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(config.binanceFuturesBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    @Named("coinglass")
    fun provideCoinglassOkHttp(
        @Named("PublicClient") base: OkHttpClient,
    ): OkHttpClient =
        base
            .newBuilder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain
                        .request()
                        .newBuilder()
                        .addHeader("CG-API-KEY", BuildConfig.COINGLASS_API_KEY)
                        .build(),
                )
            }.build()

    @Provides @Singleton
    @Named("coinglass")
    fun provideCoinglassRetrofit(
        @Named("coinglass") client: OkHttpClient,
        config: EndpointsConfig,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(config.coinglassBaseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideCoinglassApi(
        @Named("coinglass") retrofit: Retrofit,
    ): CoinglassApi = retrofit.create(CoinglassApi::class.java)

    @Provides @Singleton
    @Named("alphavantage")
    fun provideAlphaVantageRetrofit(
        @Named("PublicClient") okHttpClient: OkHttpClient,
        config: EndpointsConfig,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(config.alphaVantageBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideAlphaVantageApi(
        @Named("alphavantage") retrofit: Retrofit,
    ): AlphaVantageApi = retrofit.create(AlphaVantageApi::class.java)

    @Provides @Singleton
    @Named("coinmarketcal")
    fun provideCoinMarketCalRetrofit(
        @Named("PublicClient") okHttpClient: OkHttpClient,
        config: EndpointsConfig,
    ): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(config.coinMarketCalBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideCoinMarketCalApi(
        @Named("coinmarketcal") retrofit: Retrofit,
    ): CoinMarketCalApi = retrofit.create(CoinMarketCalApi::class.java)

    @Provides @Singleton
    fun provideDefiLlamaApi(
        @Named("PublicClient") client: OkHttpClient,
        config: EndpointsConfig,
    ): DefiLlamaApi = createRetrofit(config.defiLlamaApiBaseUrl, client).create(DefiLlamaApi::class.java)

    @Provides @Singleton
    fun provideHeliusApi(
        @Named("PublicClient") client: OkHttpClient,
        config: EndpointsConfig,
    ): HeliusApi = createRetrofit(config.heliusApiBaseUrl, client).create(HeliusApi::class.java)

    @Provides @Singleton
    fun provideMempoolSpaceApi(
        @Named("PublicClient") client: OkHttpClient,
        config: EndpointsConfig,
    ): MempoolSpaceApi = createRetrofit(config.mempoolSpaceApiBaseUrl, client).create(MempoolSpaceApi::class.java)

    @Provides @Singleton
    fun provideGson(): Gson = Gson()
}
