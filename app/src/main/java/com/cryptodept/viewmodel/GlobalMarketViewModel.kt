package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.GlobalMarketData
import com.cryptodept.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlobalMarketViewModel
    @Inject
    constructor(
        private val repository: CryptoRepository,
    ) : ViewModel() {
        val marketData: StateFlow<GlobalMarketData?> = repository.getGlobalMarketDataFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )

        fun refreshData() {
            viewModelScope.launch(Dispatchers.IO) {
                repository.getGlobalMarketData()
            }
        }
    }
