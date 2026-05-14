package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class BloombergWallViewModel @Inject constructor(
    repository: CryptoRepository
) : ViewModel() {

    // Observe top coins for real-time prices and 24h change
    val topCoins: StateFlow<List<CoinPrice>> = repository.getAllCoinPrices()
        .map { prices ->
            prices.sortedByDescending { it.marketCap }.take(15)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lastUpdate: StateFlow<Long> = topCoins.map { 
        it.firstOrNull()?.lastUpdated ?: System.currentTimeMillis() 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())
}
