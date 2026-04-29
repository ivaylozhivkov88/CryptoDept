package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.MacroData
import com.cryptodept.domain.repository.MacroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MacroViewModel @Inject constructor(
    private val macroRepository: MacroRepository
) : ViewModel() {

    private val _state = MutableStateFlow<MacroUiState>(MacroUiState.Loading)
    val state: StateFlow<MacroUiState> = _state.asStateFlow()

    init { loadMacro() }

    fun loadMacro() {
        viewModelScope.launch {
            _state.value = MacroUiState.Loading
            macroRepository.getMacroData()
                .onSuccess { _state.value = MacroUiState.Success(it) }
                .onFailure { _state.value = MacroUiState.Error(it.message ?: "MACRO LOAD FAILED") }
        }
    }
}

sealed class MacroUiState {
    object Loading : MacroUiState()
    data class Success(val data: MacroData) : MacroUiState()
    data class Error(val message: String) : MacroUiState()
}
