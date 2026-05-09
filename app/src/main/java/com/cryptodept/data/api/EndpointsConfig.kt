package com.cryptodept.data.api

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EndpointsConfig
    @Inject
    constructor() {
        var coinGeckoBaseUrl = "https://api.coingecko.com/api/v3/"
        var krakenBaseUrl = "https://api.kraken.com/0/public/"
        var coinbaseBaseUrl = "https://api.coinbase.com/api/v3/brokerage/"
        var coinCapBaseUrl = "https://api.coincap.io/v2/"
        var coinPaprikaBaseUrl = "https://api.coinpaprika.com/v1/"
        var fearGreedBaseUrl = "https://api.alternative.me/"
        var cryptoPanicBaseUrl = "https://cryptopanic.com/api/v1/"
        var etherscanBaseUrl = "https://api.etherscan.io/"
        var defiLlamaApiBaseUrl = "https://api.llama.fi/"
        var heliusApiBaseUrl = "https://api.helius.xyz/"
        var mempoolSpaceApiBaseUrl = "https://mempool.space/api/"
        var binanceFuturesBaseUrl = "https://fapi.binance.com/"
        var coinglassBaseUrl = "https://open-api.coinglass.com/"
        var alphaVantageBaseUrl = "https://www.alphavantage.co/"
        var coinMarketCalBaseUrl = "https://developers.coinmarketcal.com/"
        var blockchainBaseUrl = "https://api.blockchain.info/"
    }
