package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.DeFiProtocol
import com.cryptodept.domain.model.DeFiYieldOpportunity
import com.cryptodept.domain.model.LpSimulationResult
import com.cryptodept.domain.repository.DeFiRepository
import com.cryptodept.domain.usecase.SimulateLpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DeFiViewModel @Inject constructor(
    private val repository: DeFiRepository,
    private val simulateLpUseCase: SimulateLpUseCase,
    private val demoMode: com.cryptodept.util.DemoModeProvider,
) : ViewModel() {
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DeFiUiState> = demoMode.demoActiveState.flatMapLatest { active ->
        if (active) {
            flowOf(DeFiUiState.Success(
                protocols = listOf(
                    DeFiProtocol("lido", "Lido", "LDO", "https://lido.fi", "Liquid Staking", "", 28_000_000_000.0, 0.5, 1.2, 5.4, "Ethereum", "Liquid Staking"),
                    DeFiProtocol("aave", "Aave", "AAVE", "https://aave.com", "Lending", "", 12_000_000_000.0, -0.2, 0.8, 3.1, "Multi", "Lending")
                ),
                yields = listOf(
                    DeFiYieldOpportunity("Lido", "stETH", 28_000_000_000.0, 3.8, "Ethereum"),
                    DeFiYieldOpportunity("Aave", "USDC", 1_500_000_000.0, 5.2, "Polygon")
                )
            ))
        } else {
            _realUiState
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DeFiUiState.Loading)

    private val _realUiState = MutableStateFlow<DeFiUiState>(DeFiUiState.Loading)
    
    private val _simulationResult = MutableStateFlow<LpSimulationResult?>(null)
    val simulationResult: StateFlow<LpSimulationResult?> = _simulationResult.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _realUiState.value = DeFiUiState.Loading
            delay(500)
            try {
                withContext(Dispatchers.IO) {
                    val protocols = async { repository.getTopProtocols() }
                    val yields = async { repository.getTopYields() }

                    val protocolsRes = protocols.await()
                    val yieldsRes = yields.await()

                    if (protocolsRes.isSuccess || yieldsRes.isSuccess) {
                        val p = protocolsRes.getOrDefault(emptyList())
                        val y = yieldsRes.getOrDefault(emptyList())
                        _realUiState.value = DeFiUiState.Success(
                            protocols = p,
                            yields = y
                        )
                    } else {
                        val error = protocolsRes.exceptionOrNull()?.message ?: yieldsRes.exceptionOrNull()?.message ?: "FAILED TO FETCH DEFI DATA"
                        _realUiState.value = DeFiUiState.Error(error)
                    }
                }
            } catch (e: Exception) {
                _realUiState.value = DeFiUiState.Error(e.message ?: "UNKNOWN ERROR")
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
