package com.cryptodept.domain.model

data class CorrelationMatrix(
    val symbols: List<String>,
    val matrix: List<List<Double>>,
    val timestamp: Long = System.currentTimeMillis(),
)
