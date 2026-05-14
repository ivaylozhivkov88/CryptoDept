package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.DeFiProtocol
import com.cryptodept.domain.model.DeFiYieldOpportunity
import com.cryptodept.domain.model.LpSimulationResult
import com.cryptodept.domain.repository.DeFiRepository
import com.cryptodept.domain.usecase.SimulateLpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DeFiViewModel @Inject constructor(
    private val repository: DeFiRepository,
    private val simulateLpUseCase: SimulateLpUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow<DeFiUiState>(DeFiUiState.Loading)
    val uiState: StateFlow<DeFiUiState> = _uiState.asStateFlow()

    private val _simulationResult = MutableStateFlow<LpSimulationResult?>(null)
    val simulationResult: StateFlow<LpSimulationResult?> = _simulationResult.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = DeFiUiState.Loading
            kotlinx.coroutines.delay(500)
            try {
                withContext(Dispatchers.IO) {
                    val protocols = async { repository.getTopProtocols() }
                    val yields = async { repository.getTopYields() }

                    val protocolsRes = protocols.await()
                    val yieldsRes = yields.await()

                    if (protocolsRes.isSuccess && yieldsRes.isSuccess) {
                        _uiState.value = DeFiUiState.Success(
                            protocols = protocolsRes.getOrThrow(),
                            yields = yieldsRes.getOrThrow()
                        )
                    } else {
                        _uiState.value = DeFiUiState.Error("FAILED TO FETCH DEFI DATA")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = DeFiUiState.Error(e.message ?: "UNKNOWN ERROR")
            }
        }
    }

    fun runSimulation(
        initialInvestment: Double,
        priceChangeA: Double,
        priceChangeB: Double,
        apy: Double,
        days: Int
    ) {
        _simulationResult.value = simulateLpUseCase(
            initialInvestment,
            priceChangeA,
            priceChangeB,
            apy,
            days
        )
    }
}

sealed class DeFiUiState {
    object Loading : DeFiUiState()
    data class Success(
        val protocols: List<DeFiProtocol>,
        val yields: List<DeFiYieldOpportunity>
    ) : DeFiUiState()
    data class Error(val message: String) : DeFiUiState()
}
