package com.cryptodept.di

import android.content.Context
import androidx.room.Room
import com.cryptodept.data.db.AlertDao
import com.cryptodept.data.db.CoinDao
import com.cryptodept.data.db.CryptoDatabase
import com.cryptodept.data.db.PriceHistoryDao
import com.cryptodept.data.db.TradeJournalDao
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
    fun provideDatabase(@ApplicationContext context: Context): CryptoDatabase {
        return Room.databaseBuilder(
            context,
            CryptoDatabase::class.java,
            CryptoDatabase.DATABASE_NAME
        )
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
}