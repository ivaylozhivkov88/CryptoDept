package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.SessionStats
import com.cryptodept.domain.usecase.PsychologyAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PsychologyViewModel @Inject constructor(
    private val analyzer: PsychologyAnalyzer
) : ViewModel() {

    private val _state = MutableStateFlow<PsychologyUiState>(PsychologyUiState.Loading)
    val state: StateFlow<PsychologyUiState> = _state.asStateFlow()

    init { analyze() }

    fun analyze() {
        viewModelScope.launch {
            _state.value = PsychologyUiState.Loading
            try {
                val stats = withContext(Dispatchers.IO) { analyzer.analyzeSession() }
                _state.value = PsychologyUiState.Success(stats)
            } catch (e: Exception) {
                _state.value = PsychologyUiState.Error(e.message ?: "ANALYSIS FAILED")
            }
        }
    }
}

sealed class PsychologyUiState {
    object Loading : PsychologyUiState()
    data class Success(val stats: SessionStats) : PsychologyUiState()
    data class Error(val message: String) : PsychologyUiState()
}
