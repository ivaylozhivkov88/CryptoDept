package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.repository.CryptoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveAnalysisHistoryUseCase @Inject constructor(
    private val cryptoRepository: CryptoRepository
) {
    operator fun invoke(): Flow<List<String>> {
        return cryptoRepository.getTrackedCoinPrices()
            .map { prices -> prices.map { it.symbol.uppercase() } }
    }
}
