package com.cryptodept.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.prediction.HistoricalAccuracy
import com.cryptodept.ui.prediction.PredictionViewModel

/**
 * Adapter component that fetches accuracy and displays it using the standard [AccuracyBadge].
 */
@Composable
fun ModelAccuracyBadge(
    modelName: String,
    coinId: String,
    viewModel: PredictionViewModel = hiltViewModel(),
    compact: Boolean = true,
) {
    val accuracy by produceState<HistoricalAccuracy?>(null, modelName, coinId) {
        value = viewModel.getModelAccuracy(coinId, modelName)
    }
    
    AccuracyBadge(
        accuracyPercent = accuracy?.accuracyPercent?.toInt(),
        sampleSize = accuracy?.sampleSize,
        compact = compact
    )
}
