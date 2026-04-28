package com.cryptodept.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        CoinEntity::class,
        PriceHistoryEntity::class,
        AlertEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CryptoDatabase : RoomDatabase() {
    abstract val coinDao: CoinDao
    abstract val priceHistoryDao: PriceHistoryDao
    abstract val alertDao: AlertDao

    companion object {
        const val DATABASE_NAME = "crypto_db"
    }
}