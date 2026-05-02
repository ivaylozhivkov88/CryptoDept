package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.PortfolioEntry
import com.cryptodept.domain.model.PortfolioSummary
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val portfolioRepository: PortfolioRepository,
    private val cryptoRepository: CryptoRepository
) : ViewModel() {

    val uiState: StateFlow<PortfolioUiState> = portfolioRepository.getPortfolioSummary()
        .map { summary -> PortfolioUiState.Success(summary) as PortfolioUiState }
        .catch { emit(PortfolioUiState.Error(it.message ?: "PORTFOLIO_SYNC_FAILED")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PortfolioUiState.Loading)

    val trackedCoins = cryptoRepository.getTrackedCoinPrices()
        .map { list -> list.map { it.id to it.symbol } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addPosition(coinId: String, symbol: String, quantity: Double, price: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val entry = PortfolioEntry(
                id = UUID.randomUUID().toString(),
                coinId = coinId,
                symbol = symbol.uppercase(),
                quantity = quantity,
                averageEntryPrice = price,
                addedAt = System.currentTimeMillis()
            )
            portfolioRepository.addPosition(entry)
        }
    }

    fun removePosition(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            portfolioRepository.removePosition(id)
        }
    }
}

sealed class PortfolioUiState {
    object Loading : PortfolioUiState()
    data class Success(val summary: PortfolioSummary) : PortfolioUiState()
    data class Error(val message: String) : PortfolioUiState()
}
