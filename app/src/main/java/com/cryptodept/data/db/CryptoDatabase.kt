package com.cryptodept.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CoinEntity::class,
        PriceHistoryEntity::class,
        AlertEntity::class,
        TradeJournalEntity::class,
        PredictionAccuracyEntity::class,
        PortfolioEntity::class
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class CryptoDatabase : RoomDatabase() {
    abstract val coinDao: CoinDao
    abstract val priceHistoryDao: PriceHistoryDao
    abstract val alertDao: AlertDao
    abstract val tradeJournalDao: TradeJournalDao
    abstract val predictionAccuracyDao: PredictionAccuracyDao
    abstract val portfolioDao: PortfolioDao

    companion object {
        const val DATABASE_NAME = "crypto_db"
    }
}