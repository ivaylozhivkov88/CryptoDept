package com.cryptodept.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cryptodept.data.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            try {
                db.execSQL("ALTER TABLE trade_journal ADD COLUMN positionSizeUsd REAL")
                db.execSQL("ALTER TABLE trade_journal ADD COLUMN marketConditions TEXT NOT NULL DEFAULT ''")
            } catch (e: Exception) {
                android.util.Log.e("CryptoDept_DB", "Migration columns already exist or failed: ${e.message}")
            }
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE coins ADD COLUMN rank INTEGER NOT NULL DEFAULT 0")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_price_history_coinId_timestamp ON price_history (coinId, timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_alerts_coinId ON alerts (coinId)")
            db.execSQL("""
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
            """)
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS portfolio (
                    id TEXT PRIMARY KEY NOT NULL,
                    coinId TEXT NOT NULL,
                    symbol TEXT NOT NULL,
                    quantity REAL NOT NULL,
                    averageEntryPrice REAL NOT NULL,
                    addedAt INTEGER NOT NULL
                )
            """)
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CryptoDatabase {
        return Room.databaseBuilder(
            context,
            CryptoDatabase::class.java,
            CryptoDatabase.DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_3_4, MIGRATION_4_5)
            .fallbackToDestructiveMigration()
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
}