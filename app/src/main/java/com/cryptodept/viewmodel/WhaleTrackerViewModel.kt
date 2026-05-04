package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.api.WhaleTransaction
import com.cryptodept.domain.usecase.WhaleTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class WhaleTrackerUiState {
    object Loading : WhaleTrackerUiState()
    data class Success(val transactions: List<WhaleTransaction>) : WhaleTrackerUiState()
    data class Error(val message: String) : WhaleTrackerUiState()
}

@HiltViewModel
class WhaleTrackerViewModel @Inject constructor(
    private val whaleTracker: WhaleTracker
) : ViewModel() {

    private val _uiState = MutableStateFlow<WhaleTrackerUiState>(WhaleTrackerUiState.Loading)
    val uiState: StateFlow<WhaleTrackerUiState> = _uiState.asStateFlow()

    init {
        loadWhaleMoves()
    }

    fun loadWhaleMoves() {
        viewModelScope.launch {
            _uiState.value = WhaleTrackerUiState.Loading
            val moves = whaleTracker.getRecentWhaleMoves()
            if (moves.isNotEmpty()) {
                _uiState.value = WhaleTrackerUiState.Success(moves)
            } else {
                _uiState.value = WhaleTrackerUiState.Error("NO WHALE MOVES DETECTED OR API LIMIT REACHED")
            }
        }
    }
}