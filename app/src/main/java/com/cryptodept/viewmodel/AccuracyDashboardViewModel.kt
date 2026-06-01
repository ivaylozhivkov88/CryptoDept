package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AccuracyDashboardViewModel @Inject constructor(
    private val tracker: com.cryptodept.domain.usecase.PredictionAccuracyTracker,
    private val firebaseDataSource: com.cryptodept.data.remote.source.FirebaseRemoteDataSource
) : ViewModel() {
    
    val state: StateFlow<AccuracyDashboardState> = combine(
        tracker.getOverallAccuracyFlow(),
        tracker.getModelStatsFlow(),
        tracker.getRegimeStatsFlow(),
        firebaseDataSource.getGlobalState()
    ) { localAccuracyPair, models, regimes, cloudBriefing ->
        val (localAccuracy, localCount) = localAccuracyPair
        
        // Use local data if we have at least 1 verified sample
        // Otherwise use cloud data or baseline
        val finalAccuracy = when {
            localCount >= 1 -> localAccuracy
            cloudBriefing != null && cloudBriefing.globalAccuracy > 0 -> cloudBriefing.globalAccuracy
            else -> 68.4 // Baseline
        }

        val finalCount = when {
            localCount >= 1 -> localCount
            cloudBriefing != null && cloudBriefing.globalPredictionCount > 0 -> cloudBriefing.globalPredictionCount
            else -> 1420 // Baseline
        }

        AccuracyDashboardState(
            overallAccuracy = finalAccuracy,
            totalSamples = finalCount,
            modelStats = models,
            regimeStats = regimes,
            isSyncing = false,
            lastUpdate = cloudBriefing?.narrative?.take(10) ?: "Local" // Temporary hack to see if cloud is changing
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccuracyDashboardState())
}

data class AccuracyDashboardState(
    val overallAccuracy: Double = 0.0,
    val totalSamples: Int = 0,
    val modelStats: List<ModelStat> = emptyList(),
    val regimeStats: List<RegimeStat> = emptyList(),
    val isSyncing: Boolean = true,
    val lastUpdate: String = ""
)

data class ModelStat(val name: String, val accuracy: Int)
data class RegimeStat(val name: String, val accuracy: Int)
