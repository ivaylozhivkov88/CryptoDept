package com.cryptodept.di

import com.cryptodept.data.api.*
import com.cryptodept.BuildConfig
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

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

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
    }

    @Provides
    @Singleton
    @Named("PublicClient")
    fun providePublicOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    @Named("CoinGeckoClient")
    fun provideCoinGeckoOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("x-cg-demo-api-key", BuildConfig.COINGECKO_API_KEY)
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // --- Retrofit Providers ---

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
    fun provideCryptoPanicApi(@Named("PublicClient") client: OkHttpClient): CryptoPanicApi =
        createRetrofit(CRYPTOPANIC_BASE_URL, client).create(CryptoPanicApi::class.java)

    @Provides @Singleton
    fun provideCryptoNewsApi(@Named("PublicClient") client: OkHttpClient): CryptoNewsApi =
        createRetrofit(CRYPTONEWS_BASE_URL, client).create(CryptoNewsApi::class.java)

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
    fun provideGson(): Gson = Gson()
}
