package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.MacroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MacroViewModel
    @Inject
    constructor(
        private val macroRepository: MacroRepository,
    ) : ViewModel() {
        private val _state = MutableStateFlow<MacroUiState>(MacroUiState.Loading)
        val state: StateFlow<MacroUiState> = _state.asStateFlow()

        init {
            loadMacro()
        }

        fun loadMacro() {
            viewModelScope.launch {
                _state.value = MacroUiState.Loading
                withContext(Dispatchers.IO) {
                    try {
                        coroutineScope {
                            val macroData = async { macroRepository.getMacroData() }.await()
                            val correlations = async { macroRepository.getMacroCorrelations() }.await()

                            if (macroData.isSuccess && correlations.isSuccess) {
                                _state.value =
                                    MacroUiState.Success(
                                        data = macroData.getOrThrow(),
                                        correlations = correlations.getOrThrow(),
                                    )
                            } else {
                                _state.value = MacroUiState.Error("PARTIAL DATA FAILURE")
                            }
                        }
                    } catch (e: Exception) {
                        _state.value = MacroUiState.Error(e.message ?: "MACRO LOAD FAILED")
                    }
                }
            }
        }
    }

sealed class MacroUiState {
    object Loading : MacroUiState()

    data class Success(
        val data: MacroData,
        val correlations: List<MacroCorrelation> = emptyList(),
    ) : MacroUiState()

    data class Error(
        val message: String,
    ) : MacroUiState()
}
