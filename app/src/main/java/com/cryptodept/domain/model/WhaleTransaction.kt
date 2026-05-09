package com.cryptodept.domain.model

enum class Blockchain {
    ETHEREUM,
    SOLANA,
    BITCOIN,
}

data class WhaleTransaction(
    val id: String,
    val blockchain: Blockchain,
    val amount: Double,
    val amountUsd: Double,
    val symbol: String,
    val fromAddress: String,
    val toAddress: String,
    val timestamp: Long,
    val transactionHash: String,
    val isExchange: Boolean = false,
    val exchangeName: String? = null,
)
