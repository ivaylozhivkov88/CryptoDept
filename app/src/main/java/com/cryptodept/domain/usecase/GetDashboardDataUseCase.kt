package com.cryptodept.domain.usecase

import com.cryptodept.data.datastore.SubscriptionAccessManager
import com.cryptodept.domain.model.DashboardData
import com.cryptodept.domain.repository.CryptoRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class GetDashboardDataUseCase
    @Inject
    constructor(
        private val repository: CryptoRepository,
        private val subscription: SubscriptionAccessManager,
    ) {
        operator fun invoke(): Flow<DashboardData> =
            combine(
                repository.getTrackedCoinPrices(),
                subscription.isAdmin,
            ) { prices, isAdmin ->
                DashboardData(
                    prices = prices,
                    networkHealth = null, // To be filled by VM or updated flow
                    aiSummary = "READY",
                    isAdmin = isAdmin,
                )
            }
    }
