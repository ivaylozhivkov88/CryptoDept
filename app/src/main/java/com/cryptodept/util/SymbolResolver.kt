package com.cryptodept.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SymbolResolver @Inject constructor() {
    
    /**
     * Converts common symbols or internal names to CoinGecko IDs.
     */
    fun toCoinGeckoId(input: String): String {
        return when (input.uppercase()) {
            "BTC", "BITCOIN" -> "bitcoin"
            "ETH", "ETHEREUM" -> "ethereum"
            "BNB", "BINANCECOIN" -> "binancecoin"
            "SOL", "SOLANA" -> "solana"
            "XRP", "RIPPLE" -> "ripple"
            "DOGE", "DOGECOIN" -> "dogecoin"
            "ADA", "CARDANO" -> "cardano"
            "TRX", "TRON" -> "tron"
            "MATIC", "POLYGON" -> "matic-network"
            "DOT", "POLKADOT" -> "polkadot"
            "LTC", "LITECOIN" -> "litecoin"
            "AVAX", "AVALANCHE" -> "avalanche-2"
            "LINK", "CHAINLINK" -> "chainlink"
            "UNI", "UNISWAP" -> "uniswap"
            "ATOM", "COSMOS" -> "cosmos"
            else -> input.lowercase()
        }
    }

    /**
     * Converts common symbols or names to Binance USDT symbols (e.g. BTCUSDT).
     */
    fun toBinanceSymbol(input: String): String {
        val base = when (input.uppercase()) {
            "BITCOIN" -> "BTC"
            "ETHEREUM" -> "ETH"
            "LITECOIN" -> "LTC"
            "RIPPLE" -> "XRP"
            "BINANCECOIN" -> "BNB"
            "SOLANA" -> "SOL"
            "CARDANO" -> "ADA"
            "DOGECOIN" -> "DOGE"
            "TRON" -> "TRX"
            "POLKADOT" -> "DOT"
            "CHAINLINK" -> "LINK"
            "AVALANCHE" -> "AVAX"
            "POLYGON" -> "MATIC"
            else -> input.uppercase()
        }
        return if (base.endsWith("USDT")) base else "${base}USDT"
    }

    /**
     * Standardizes the symbol for Coinglass.
     */
    fun toCoinglassSymbol(input: String): String {
        return when (input.uppercase()) {
            "BITCOIN" -> "BTC"
            "ETHEREUM" -> "ETH"
            "BINANCECOIN" -> "BNB"
            "DOGECOIN" -> "DOGE"
            "CARDANO" -> "ADA"
            "LITECOIN" -> "LTC"
            else -> input.uppercase()
        }
    }
}
