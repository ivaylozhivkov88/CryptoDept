package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.GlobalMarketData
import com.cryptodept.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlobalMarketViewModel @Inject constructor(
    private val repository: CryptoRepository
) : ViewModel() {

    private val _marketData = MutableStateFlow<GlobalMarketData?>(null)
    val marketData = _marketData.asStateFlow()

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                repository.getGlobalMarketData().onSuccess {
                    _marketData.value = it
                }
                delay(60000) // Refresh every 60 seconds
            }
        }
    }
}
