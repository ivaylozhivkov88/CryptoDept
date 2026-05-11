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
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE trade_journal ADD COLUMN positionSizeUsd REAL")
                    db.execSQL("ALTER TABLE trade_journal ADD COLUMN marketConditions TEXT NOT NULL DEFAULT ''")
                } catch (e: Exception) {
                    android.util.Log.e("CryptoDept_DB", "Migration columns already exist or failed: ${e.message}")
                }
            }
        }

    private val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE coins ADD COLUMN rank INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_price_history_coinId_timestamp ON price_history (coinId, timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_alerts_coinId ON alerts (coinId)")
                db.execSQL(
                    """
                CREATE TABLE IF NOT EXISTS prediction_accuracy (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    coinId TEXT NOT NULL,
                    model TEXT NOT NULL,
                    predictedDirection TEXT NOT NULL,
                    actualDirection TEXT,
                    predictedAt INTEGER NOT NULL,
                    verifiedAt INTEGER,
                    wasCorrect INTEGER,
                    confidenceAtPrediction REAL NOT NULL
                )
            """,
                )
            }
        }

    private val MIGRATION_4_5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                CREATE TABLE IF NOT EXISTS portfolio (
                    id TEXT PRIMARY KEY NOT NULL,
                    coinId TEXT NOT NULL,
                    symbol TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    averageEntryPrice REAL NOT NULL,
                    addedAt INTEGER NOT NULL
                )
            """,
                )
            }
        }

    private val MIGRATION_5_6 =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                CREATE TABLE IF NOT EXISTS custom_signal_rules (
                    id TEXT PRIMARY KEY NOT NULL,
                    name TEXT NOT NULL,
                    conditionsJson TEXT NOT NULL,
                    operator TEXT NOT NULL,
                    action TEXT NOT NULL,
                    isActive INTEGER NOT NULL
                )
            """,
                )
            }
        }

    private val MIGRATION_6_7 =
        object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alerts ADD COLUMN name TEXT")
                db.execSQL("ALTER TABLE alerts ADD COLUMN conditionsJson TEXT")
                db.execSQL("ALTER TABLE alerts ADD COLUMN logicOperator TEXT")
                db.execSQL("ALTER TABLE alerts ADD COLUMN cooldownMinutes INTEGER NOT NULL DEFAULT 60")
                db.execSQL("ALTER TABLE alerts ADD COLUMN lastTriggeredAt INTEGER")
            }
        }

    private val MIGRATION_7_8 =
        object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                CREATE TABLE IF NOT EXISTS news (
                    id TEXT PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL,
                    url TEXT NOT NULL,
                    source TEXT NOT NULL,
                    publishedAt INTEGER NOT NULL,
                    sentiment TEXT NOT NULL,
                    currencies TEXT NOT NULL
                )
            """,
                )
            }
        }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        securePrefs: SecurePrefsService,
    ): CryptoDatabase {
        val passphrase = securePrefs.getDatabasePassword()
        val factory = net.sqlcipher.database.SupportFactory(passphrase)

        return Room
            .databaseBuilder(
                context,
                CryptoDatabase::class.java,
                CryptoDatabase.DATABASE_NAME,
            ).openHelperFactory(factory)
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
            ).fallbackToDestructiveMigration(false)
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
}
