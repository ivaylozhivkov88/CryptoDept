package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.api.FearGreedApi
import com.cryptodept.data.api.FearGreedData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class FearGreedUiState {
    object Loading : FearGreedUiState()
    data class Success(val current: FearGreedData, val history: List<FearGreedData>) : FearGreedUiState()
    data class Error(val message: String) : FearGreedUiState()
}

@HiltViewModel
class FearGreedViewModel @Inject constructor(
    private val api: FearGreedApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<FearGreedUiState>(FearGreedUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = FearGreedUiState.Loading
            withContext(Dispatchers.IO) {
                try {
                    val response = api.getFearGreedIndex(limit = 30)
                    if (response.data.isNotEmpty()) {
                        _uiState.value = FearGreedUiState.Success(
                            current = response.data.first(),
                            history = response.data
                        )
                    } else {
                        _uiState.value = FearGreedUiState.Error("NO DATA RECEIVED")
                    }
                } catch (e: Exception) {
                    _uiState.value = FearGreedUiState.Error(e.message ?: "UNKNOWN ERROR")
                }
            }
        }
    }
}
