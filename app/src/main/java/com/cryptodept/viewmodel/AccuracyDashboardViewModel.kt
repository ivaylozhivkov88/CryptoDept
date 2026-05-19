package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AccuracyDashboardViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(AccuracyDashboardState())
    val state: StateFlow<AccuracyDashboardState> = _state.asStateFlow()
}

data class AccuracyDashboardState(
    val overallAccuracy: Double = 68.4,
    val totalSamples: Int = 1420,
    val modelStats: List<ModelStat> = listOf(
        ModelStat("FFT Harmonic", 72),
        ModelStat("Monte Carlo", 65),
        ModelStat("Wyckoff Cycle", 70),
        ModelStat("Elliott Wave", 63)
    )
)

data class ModelStat(val name: String, val accuracy: Int)
