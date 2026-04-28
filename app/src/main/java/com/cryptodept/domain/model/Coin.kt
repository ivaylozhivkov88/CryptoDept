package com.cryptodept.domain.model

data class Coin(
    val id: String,
    val symbol: String,
    val name: String,
    val isTracked: Boolean = false
)