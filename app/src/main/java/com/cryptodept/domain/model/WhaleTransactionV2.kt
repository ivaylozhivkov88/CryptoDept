package com.cryptodept.domain.model

/**
 * Enhanced whale transaction model for DIY Tracking.
 */
data class WhaleTransactionV2(
    val hash: String,
    val blockchain: String,
    val symbol: String,
    val amount: Double,
    val amountUsd: Double,
    val fromAddress: String,
    val toAddress: String,
    val fromOwner: String?,
    val toOwner: String?,
    val transactionType: TransactionType,
    val timestamp: Long,
    val explorerUrl: String?,
    val significance: WhaleSignificance,
)

enum class TransactionType(val displayName: String, val emoji: String) {
    TRANSFER("Transfer", "↔️"),
    EXCHANGE_DEPOSIT("Exchange Deposit", "📥"),
    EXCHANGE_WITHDRAWAL("Exchange Withdrawal", "📤"),
    EXCHANGE_TO_EXCHANGE("Exchange ↔ Exchange", "🔄"),
    UNKNOWN("Unknown", "❓");
    
    companion object {
        fun classify(fromOwner: String?, toOwner: String?): TransactionType {
            val fromIsExchange = fromOwner?.lowercase()?.contains("binance") == true || fromOwner?.lowercase()?.contains("coinbase") == true
            val toIsExchange = toOwner?.lowercase()?.contains("binance") == true || toOwner?.lowercase()?.contains("coinbase") == true
            
            return when {
                fromIsExchange && toIsExchange -> EXCHANGE_TO_EXCHANGE
                !fromIsExchange && toIsExchange -> EXCHANGE_DEPOSIT
                fromIsExchange && !toIsExchange -> EXCHANGE_WITHDRAWAL
                else -> TRANSFER
            }
        }
    }
}

enum class WhaleSignificance(val minUsd: Double, val label: String, val emoji: String) {
    MEGA(50_000_000.0, "MEGA WHALE", "🐋🐋🐋"),
    LARGE(10_000_000.0, "LARGE", "🐋🐋"),
    SIGNIFICANT(1_000_000.0, "SIGNIFICANT", "🐋"),
    NOTABLE(500_000.0, "NOTABLE", "🐟");
    
    companion object {
        fun fromAmount(usd: Double): WhaleSignificance = when {
            usd >= MEGA.minUsd -> MEGA
            usd >= LARGE.minUsd -> LARGE
            usd >= 1_000_000.0 -> SIGNIFICANT
            else -> NOTABLE
        }
    }
}
