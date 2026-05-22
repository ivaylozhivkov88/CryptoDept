package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.WhaleTransactionV2
import com.cryptodept.domain.usecase.whale.AggregateWhaleActivityUseCase
import com.cryptodept.util.DemoModeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WhaleViewModel @Inject constructor(
    private val aggregator: AggregateWhaleActivityUseCase,
    private val demoMode: DemoModeProvider,
    private val haptic: com.cryptodept.util.HapticService,
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _transactions = MutableStateFlow<List<WhaleTransactionV2>>(emptyList())
    val transactions: StateFlow<List<WhaleTransactionV2>> = _transactions.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (demoMode.isActive()) {
                // Mock data for demo
                return@launch
            }
            _isRefreshing.value = true
            try {
                val results = aggregator.execute(minUsd = 500_000.0)
                if (results.isNotEmpty() && results.size > _transactions.value.size) {
                    haptic.whaleAlert()
                }
                _transactions.value = results
            } catch (e: Exception) {
                android.util.Log.e("WhaleViewModel", "Failed to fetch whales", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
