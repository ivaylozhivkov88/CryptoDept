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
        ModelStat("Elliott Wave", 63),
        ModelStat("Hurst Exponent", 68),
        ModelStat("Fractal Dimension", 66)
    ),
    val regimeStats: List<RegimeStat> = listOf(
        RegimeStat("Bullish (High Vol)", 74),
        RegimeStat("Bearish (Extreme Fear)", 61),
        RegimeStat("Crab (Consolidation)", 55)
    )
)

data class ModelStat(val name: String, val accuracy: Int)
data class RegimeStat(val name: String, val accuracy: Int)
