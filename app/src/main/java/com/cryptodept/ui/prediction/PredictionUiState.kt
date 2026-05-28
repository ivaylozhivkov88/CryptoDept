package com.cryptodept.ui.prediction

import com.cryptodept.domain.model.PricePrediction

sealed class PredictUiState {
    object Idle : PredictUiState()

    data class Loading(
        val logs: List<String>,
        val progress: Float,
    ) : PredictUiState()

    data class Success(
        val prediction: PricePrediction,
    ) : PredictUiState()

    data class Error(
        val message: String,
    ) : PredictUiState()
}
