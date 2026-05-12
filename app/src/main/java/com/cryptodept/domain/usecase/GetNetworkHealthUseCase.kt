package com.cryptodept.domain.usecase

import com.cryptodept.BuildConfig
import com.cryptodept.data.api.BlockchainApi
import com.cryptodept.data.api.EtherscanApi
import com.cryptodept.data.api.FearGreedApi
import com.cryptodept.data.api.GasOracleResult
import com.cryptodept.domain.model.NetworkHealth
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetNetworkHealthUseCase
    @Inject
    constructor(
        private val blockchainApi: BlockchainApi,
        private val etherscanApi: EtherscanApi,
        private val fearGreedApi: FearGreedApi,
        private val sentimentAnalyzer: SentimentAnalyzer,
        private val gson: Gson,
    ) {
        suspend operator fun invoke(): Result<NetworkHealth> =
            coroutineScope {
                try {
                    val btcStats = async { 
                        runCatching { withTimeoutOrNull(10000) { blockchainApi.getStats() } }.getOrNull() 
                    }
                    val ethGas =
                        async {
                            runCatching { withTimeoutOrNull(5000) { etherscanApi.getGasOracle(apiKey = BuildConfig.ETHERSCAN_API_KEY) } }.getOrNull()
                        }
                    val fearGreed =
                        async {
                            runCatching { withTimeoutOrNull(5000) { fearGreedApi.getFearGreedIndex() } }.getOrNull()
                        }

                    val btc = btcStats.await()
                    val eth = ethGas.await()
                    val fg = fearGreed.await()

                    var safeGasPrice = "N/A"
                    if (eth != null && eth.status == "1" && eth.result != null) {
                        val gasData = gson.fromJson(eth.result, GasOracleResult::class.java)
                        safeGasPrice = "${gasData.SafeGasPrice} Gwei"
                    }

                    val fgValue = fg?.data?.firstOrNull()
                    val fearGreedIndex = fgValue?.value?.toIntOrNull() ?: 50
                    val fearGreedLabel = fgValue?.valueClassification ?: "NEUTRAL"

                    val hashrateStr = if (btc != null) "${(btc.hash_rate / 1e18).toInt()} EH/s" else "N/A"
                    val mempoolStr = if (btc != null) "${btc.mempool_count} TXs" else "N/A"

                    val pulse = sentimentAnalyzer.calculatePulse("BTC")
                    val pulseLabel = sentimentAnalyzer.getPulseLabel(pulse)

                    Result.success(
                        NetworkHealth(
                            btcHashrate = hashrateStr,
                            btcMempool = mempoolStr,
                            ethGas = safeGasPrice,
                            fearGreedIndex = fearGreedIndex,
                            fearGreedLabel = fearGreedLabel,
                            socialPulse = pulse,
                            socialPulseLabel = pulseLabel,
                        ),
                    )
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
    }
