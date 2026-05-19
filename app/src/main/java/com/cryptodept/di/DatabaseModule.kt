package com.cryptodept.di

import android.content.Context
import androidx.room.Room
import com.cryptodept.data.db.*
import com.cryptodept.util.SecurePrefsService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        securePrefs: SecurePrefsService,
    ): CryptoDatabase {
        val dbName = CryptoDatabase.DATABASE_NAME
        val passphrase = securePrefs.getDatabasePassword()
        val factory = net.sqlcipher.database.SupportFactory(passphrase)

        return try {
            buildDatabase(context, factory)
        } catch (e: Exception) {
            android.util.Log.e("DatabaseModule", "Database decryption failed. Wiping and recreating...", e)
            context.deleteDatabase(dbName)
            buildDatabase(context, factory)
        }
    }

    private fun buildDatabase(context: Context, factory: net.sqlcipher.database.SupportFactory): CryptoDatabase {
        return Room
            .databaseBuilder(
                context,
                CryptoDatabase::class.java,
                CryptoDatabase.DATABASE_NAME,
            ).openHelperFactory(factory)
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun provideCoinDao(db: CryptoDatabase): CoinDao = db.coinDao

    @Provides
    fun providePriceHistoryDao(db: CryptoDatabase): PriceHistoryDao = db.priceHistoryDao

    @Provides
    fun provideAlertDao(db: CryptoDatabase): AlertDao = db.alertDao

    @Provides
    fun provideTradeJournalDao(db: CryptoDatabase): TradeJournalDao = db.tradeJournalDao

    @Provides
    fun providePredictionAccuracyDao(db: CryptoDatabase): PredictionAccuracyDao = db.predictionAccuracyDao

    @Provides
    fun providePortfolioDao(db: CryptoDatabase): PortfolioDao = db.portfolioDao

    @Provides
    fun provideCustomSignalDao(db: CryptoDatabase): CustomSignalDao = db.customSignalDao

    @Provides
    fun provideNewsDao(db: CryptoDatabase): NewsDao = db.newsDao

    @Provides
    fun provideNetworkHealthDao(db: CryptoDatabase): NetworkHealthDao = db.networkHealthDao

    @Provides
    fun provideIntelligenceBriefingDao(db: CryptoDatabase): IntelligenceBriefingDao = db.intelligenceBriefingDao
}
