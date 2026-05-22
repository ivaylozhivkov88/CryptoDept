package com.cryptodept.data.api

import com.cryptodept.data.remote.source.FirebaseRemoteDataSource
import com.cryptodept.domain.repository.PriceProvider
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class FirebasePriceProvider @Inject constructor(
    private val firebaseDataSource: FirebaseRemoteDataSource
) : PriceProvider {
    override val providerName: String = "Cloud"

    override suspend fun fetchPrice(coinGeckoId: String): Result<Double> {
        return try {
            // Вземаме последния наличен стейт от облака
            val state = firebaseDataSource.getTerminalState().firstOrNull()
            // Търсим цената за конкретната монета в облачната мапа
            val price = state?.marketData?.get(coinGeckoId)?.currentPrice
            
            if (price != null && price > 0) {
                Result.success(price)
            } else {
                Result.failure(Exception("Price not available in Cloud for $coinGeckoId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
