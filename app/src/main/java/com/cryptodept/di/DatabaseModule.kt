package com.cryptodept.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        // КРИТИЧНО: Ръчно зареждаме native библиотеката на SQLCipher
        System.loadLibrary("sqlcipher")

        val dbName = CryptoDatabase.DATABASE_NAME
        val passphrase = securePrefs.getDatabasePassword()
        val factory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(passphrase)

        return try {
            buildDatabase(context, factory)
        } catch (e: Exception) {
            android.util.Log.e("DatabaseModule", "Database decryption failed. Wiping and recreating...", e)
            context.deleteDatabase(dbName)
            buildDatabase(context, factory)
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prediction_accuracy_coinId_model ON prediction_accuracy (coinId, model)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prediction_accuracy_verifiedAt ON prediction_accuracy (verifiedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prediction_accuracy_coinId ON prediction_accuracy (coinId)")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Indices are already defined in the entity for version 4
            // Room will handle creation if table is new, but for migration we ensure they exist
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prediction_accuracy_coinId_model ON prediction_accuracy (coinId, model)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prediction_accuracy_verifiedAt ON prediction_accuracy (verifiedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_prediction_accuracy_coinId ON prediction_accuracy (coinId)")
        }
    }

    private fun buildDatabase(context: Context, factory: net.zetetic.database.sqlcipher.SupportOpenHelperFactory): CryptoDatabase {
        return Room
            .databaseBuilder(
                context,
                CryptoDatabase::class.java,
                CryptoDatabase.DATABASE_NAME,
            ).openHelperFactory(factory)
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
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
