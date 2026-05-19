package com.cryptodept.data.whales

/**
 * Known whale wallets across major blockchains.
 */
object KnownWallets {
    
    data class WhaleEntity(
        val name: String,
        val category: WalletCategory,
        val address: String,
        val chain: Chain,
    )
    
    enum class Chain { BTC, ETH, SOL }
    
    enum class WalletCategory(val emoji: String, val label: String) {
        EXCHANGE_HOT("🏦", "Exchange Hot Wallet"),
        EXCHANGE_COLD("🧊", "Exchange Cold Storage"),
        WHALE_INDIVIDUAL("🐋", "Known Whale"),
        INSTITUTION("🏛️", "Institution"),
        DEFI_PROTOCOL("⚡", "DeFi Protocol"),
        BRIDGE("🌉", "Cross-Chain Bridge"),
    }
    
    val ETH_WALLETS: List<WhaleEntity> = listOf(
        WhaleEntity("Binance Hot Wallet 14", WalletCategory.EXCHANGE_HOT, "0x28C6c06298d514Db089934071355E5743bf21d60", Chain.ETH),
        WhaleEntity("Binance Cold Wallet", WalletCategory.EXCHANGE_COLD, "0xDFd5293D8e347dFe59E90eFd55b2956a1343963d", Chain.ETH),
        WhaleEntity("Coinbase Hot Wallet", WalletCategory.EXCHANGE_HOT, "0x71660c4005BA85c37ccec55d0C4493E66Fe775d3", Chain.ETH),
        WhaleEntity("Vitalik Buterin", WalletCategory.WHALE_INDIVIDUAL, "0xd8dA6BF26964aF9D7eEd9e03E53415D37aA96045", Chain.ETH)
    )
    
    val BTC_ADDRESSES: List<WhaleEntity> = listOf(
        WhaleEntity("Binance Cold Wallet", WalletCategory.EXCHANGE_COLD, "34xp4vRoCGJym3xR7yCVPFHoCNxv4Twseo", Chain.BTC),
        WhaleEntity("Bitfinex Cold Wallet", WalletCategory.EXCHANGE_COLD, "bc1qgdjqv0av3q56jvd82tkdjpy7gdp9ut8tlqmgrpmv24sq90ecnvqqjwvw97", Chain.BTC)
    )
    
    val SOL_WALLETS: List<WhaleEntity> = listOf(
        WhaleEntity("Binance Solana Hot", WalletCategory.EXCHANGE_HOT, "9WzDXwBbmkg8ZTbNMqUxvQRAyrZzDsGYdLVL9zYtAWWM", Chain.SOL),
        WhaleEntity("Coinbase Solana", WalletCategory.EXCHANGE_HOT, "H8sMJSCQxfKiFTCfDR3DUMLPwcRbM61LGFJ8N4dK3WjS", Chain.SOL)
    )
    
    fun getAllWallets(): List<WhaleEntity> = ETH_WALLETS + BTC_ADDRESSES + SOL_WALLETS
    
    fun lookupName(address: String): WhaleEntity? = 
        getAllWallets().firstOrNull { it.address.equals(address, ignoreCase = true) }
}
