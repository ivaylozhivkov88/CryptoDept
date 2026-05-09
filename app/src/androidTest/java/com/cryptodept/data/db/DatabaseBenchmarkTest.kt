package com.cryptodept.data.db

import android.content.Context
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseBenchmarkTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var db: CryptoDatabase
    private lateinit var dao: CoinDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, CryptoDatabase::class.java).build()
        dao = db.coinDao
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun benchmarkInsert1000Coins() {
        val coins = List(1000) { i ->
            CoinEntity(
                id = "coin_$i",
                symbol = "C$i",
                name = "Coin $i",
                isTracked = true,
                currentPrice = 100.0 + i
            )
        }
        
        benchmarkRule.measureRepeated {
            runBlocking {
                dao.insertCoins(coins)
            }
        }
    }

    @Test
    fun benchmarkQueryTrackedCoins() {
        val coins = List(500) { i ->
            CoinEntity(
                id = "coin_$i",
                symbol = "C$i",
                name = "Coin $i",
                isTracked = true
            )
        }
        runBlocking { dao.insertCoins(coins) }

        benchmarkRule.measureRepeated {
            runBlocking {
                dao.getTrackedCoinsCount()
            }
        }
    }
}
