package com.cryptodept.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CoinEntity::class,
        PriceHistoryEntity::class,
        AlertEntity::class,
        TradeJournalEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CryptoDatabase : RoomDatabase() {
    abstract val coinDao: CoinDao
    abstract val priceHistoryDao: PriceHistoryDao
    abstract val alertDao: AlertDao
    abstract val tradeJournalDao: TradeJournalDao

    companion object {
        const val DATABASE_NAME = "crypto_db"
    }
}