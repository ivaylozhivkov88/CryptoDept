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
            "XLM", "STELLAR" -> "stellar"
            "HBAR", "HEDERA" -> "hedera-hashgraph"
            "SHIB", "SHIBA INU" -> "shiba-inu"
            "TON", "TONCOIN" -> "the-open-network"
            "SUI" -> "sui"
            else -> input.lowercase().replace(" ", "-")
        }
    }

    /**
     * Converts an internal ID or technical name back to a clean Display Symbol.
     * E.g. "ripple" -> "XRP", "bitcoin" -> "BTC"
     */
    fun toDisplayName(id: String): String {
        return when (id.lowercase()) {
            "bitcoin" -> "BTC"
            "ethereum" -> "ETH"
            "ripple" -> "XRP"
            "solana" -> "SOL"
            "binancecoin" -> "BNB"
            "dogecoin" -> "DOGE"
            "cardano" -> "ADA"
            "tron" -> "TRX"
            "polkadot" -> "DOT"
            "litecoin" -> "LTC"
            "chainlink" -> "LINK"
            "avalanche-2" -> "AVAX"
            "matic-network" -> "MATIC"
            "uniswap" -> "UNI"
            "cosmos" -> "ATOM"
            "stellar" -> "XLM"
            "hedera-hashgraph" -> "HBAR"
            "shiba-inu" -> "SHIB"
            "the-open-network" -> "TON"
            else -> id.uppercase().replace("-", "")
        }
    }

    /**
     * Standardizes the Currency Name (Removes company names like "Ripple Labs" or "Stellar Foundation").
     */
    fun toCleanName(id: String, originalName: String): String {
        return when (id.lowercase()) {
            "bitcoin" -> "Bitcoin"
            "ethereum" -> "Ethereum"
            "ripple" -> "XRP"
            "solana" -> "Solana"
            "binancecoin" -> "BNB"
            "dogecoin" -> "Dogecoin"
            "cardano" -> "Cardano"
            "tron" -> "Tron"
            "polkadot" -> "Polkadot"
            "litecoin" -> "Litecoin"
            "chainlink" -> "Chainlink"
            "avalanche-2" -> "Avalanche"
            "matic-network" -> "Polygon"
            "uniswap" -> "Uniswap"
            "cosmos" -> "Cosmos"
            "stellar" -> "Stellar"
            "hedera-hashgraph" -> "Hedera"
            "shiba-inu" -> "Shiba Inu"
            "the-open-network" -> "TON"
            else -> originalName.replace(" Labs", "").replace(" Foundation", "").replace(" Network", "").trim()
        }
    }

    /**
     * Converts common symbols or names to Binance USDT symbols (e.g. BTCUSDT).
     */
    fun toBinanceSymbol(input: String): String {
        val base = when (input.lowercase()) {
            "bitcoin" -> "BTC"
            "ethereum" -> "ETH"
            "litecoin" -> "LTC"
            "ripple" -> "XRP"
            "binancecoin" -> "BNB"
            "solana" -> "SOL"
            "cardano" -> "ADA"
            "dogecoin" -> "DOGE"
            "tron" -> "TRX"
            "polkadot" -> "DOT"
            "chainlink" -> "LINK"
            "avalanche-2" -> "AVAX"
            "matic-network" -> "MATIC"
            "stellar" -> "XLM"
            "cosmos" -> "ATOM"
            "hedera-hashgraph" -> "HBAR"
            "shiba-inu" -> "SHIB"
            "the-open-network" -> "TON"
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
            "SOLANA" -> "SOL"
            "RIPPLE" -> "XRP"
            "DOGECOIN" -> "DOGE"
            "CARDANO" -> "ADA"
            "LITECOIN" -> "LTC"
            "POLYGON" -> "MATIC"
            "CHAINLINK" -> "LINK"
            "POLKADOT" -> "DOT"
            "TRON" -> "TRX"
            "AVALANCHE" -> "AVAX"
            else -> input.uppercase()
        }
    }

    fun toKrakenSymbol(coinGeckoId: String): String? =
        when (coinGeckoId) {
            "bitcoin" -> "XBTUSD"
            "ethereum" -> "ETHUSD"
            "ripple" -> "XRPUSD"
            "solana" -> "SOLUSD"
            "cardano" -> "ADAUSD"
            "polkadot" -> "DOTUSD"
            "dogecoin" -> "DOGEUSD"
            "chainlink" -> "LINKUSD"
            "shiba-inu" -> "SHIBUSD"
            "litecoin" -> "LTCUSD"
            "avalanche-2" -> "AVAXUSD"
            "tron" -> "TRXUSD"
            "matic-network" -> "MATICUSD"
            "stellar" -> "XLMUSD"
            "cosmos" -> "ATOMUSD"
            else -> "${coinGeckoId.uppercase()}USD"
        }

    fun toCoinbaseSymbol(coinGeckoId: String): String? =
        when (coinGeckoId) {
            "bitcoin" -> "BTC-USD"
            "ethereum" -> "ETH-USD"
            "ripple" -> "XRP-USD"
            "solana" -> "SOL-USD"
            "cardano" -> "ADA-USD"
            "polkadot" -> "DOT-USD"
            "dogecoin" -> "DOGE-USD"
            "chainlink" -> "LINK-USD"
            "shiba-inu" -> "SHIB-USD"
            "litecoin" -> "LTC-USD"
            "avalanche-2" -> "AVAX-USD"
            "tron" -> "TRX-USD"
            "matic-network" -> "MATIC-USD"
            "stellar" -> "XLM-USD"
            "cosmos" -> "ATOM-USD"
            else -> "${coinGeckoId.uppercase()}-USD"
        }

    fun toCoinCapId(coinGeckoId: String): String =
        when (coinGeckoId) {
            "avalanche-2" -> "avalanche"
            "matic-network" -> "polygon"
            else -> coinGeckoId.lowercase()
        }

    fun toCoinPaprikaId(coinGeckoId: String): String? =
        when (coinGeckoId) {
            "bitcoin" -> "btc-bitcoin"
            "ethereum" -> "eth-ethereum"
            "ripple" -> "xrp-ripple"
            "solana" -> "sol-solana"
            "cardano" -> "ada-cardano"
            "polkadot" -> "dot-polkadot"
            "dogecoin" -> "doge-dogecoin"
            "chainlink" -> "link-chainlink"
            "shiba-inu" -> "shib-shiba-inu"
            "litecoin" -> "ltc-litecoin"
            "avalanche-2" -> "avax-avalanche"
            "tron" -> "trx-tron"
            "matic-network" -> "matic-polygon"
            "stellar" -> "xlm-stellar"
            "cosmos" -> "atom-cosmos"
            else -> null // Unknown coin — return null to signal skip
        }
}
