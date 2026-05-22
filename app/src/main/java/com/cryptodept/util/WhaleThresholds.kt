package com.cryptodept.util

object WhaleThresholds {
    // BTC whales start at $1M
    const val BTC_USD = 1_000_000.0   
    
    // ETH DeFi noise starts below $2M
    const val ETH_USD = 2_000_000.0   
    
    // SOL market is smaller
    const val SOL_USD = 500_000.0     
    
    const val DEFAULT_USD = 1_000_000.0
}
