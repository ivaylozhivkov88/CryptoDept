package com.cryptodept.data.api

import com.cryptodept.BuildConfig
import com.cryptodept.domain.model.Blockchain
import com.cryptodept.domain.model.WhaleTransaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EtherscanWhaleClient
    @Inject
    constructor(
        private val etherscanApi: EtherscanApi,
        private val gson: Gson,
    ) {
        private val whaleAddresses =
            listOf(
                "0x00000000219ab540356cBB839Cbe05303d7705Fa", // Beacon Deposit Contract
                "0xC02aaA39b223FE8D0A0e5C4F27eAD9083C756Cc2", // WETH
                "0xDA9dfA130Df4dE4673b89022EE50ff26f6EA73Cf", // Binance 7
                "0xBE0eB53F46cd790Cd13851d5EFf43D12404d33E8", // Binance 8
                "0x47ac0Fb4F2D84898e4D9E7b4DaB3C24507a6D503", // Binance
                "0x73bceb1cd57c711fbc44256aa8a590804418577e", // Bitfinex 1
                "0x53d15306658af5d8524dec5003058c679a8BB611", // Bitfinex 2
            )

        suspend fun fetchWhaleTransactions(): List<WhaleTransaction> {
            val apiKey = BuildConfig.ETHERSCAN_API_KEY
            if (apiKey.isBlank()) return emptyList()

            val allTransactions = mutableListOf<WhaleTransaction>()

            // Check top 3 to stay within free tier limits easily in one burst
            whaleAddresses.take(3).forEach { address ->
                try {
                    val response =
                        etherscanApi.getTransactionList(
                            address = address,
                            apiKey = apiKey,
                            offset = 20, // Only last 20 txs
                        )
                    if (response.status == "1") {
                        val listType = object : TypeToken<List<EtherscanTxDTO>>() {}.type
                        val txs: List<EtherscanTxDTO> = gson.fromJson(response.result, listType)

                        txs
                            .filter {
                                it.isError == "0" && (it.value.toDoubleOrNull() ?: 0.0) / 1e18 > 500 // > 500 ETH
                            }.forEach { dto ->
                                allTransactions.add(dto.toDomain())
                            }
                    }
                } catch (e: Exception) {
                    // Silently fail for now
                }
            }
            return allTransactions.distinctBy { it.id }
        }
    }

data class EtherscanTxDTO(
    val hash: String,
    val from: String,
    val to: String,
    val value: String,
    val timeStamp: String,
    val isError: String,
) {
    fun toDomain(): WhaleTransaction {
        val ethValue = (value.toDoubleOrNull() ?: 0.0) / 1e18
        return WhaleTransaction(
            id = hash,
            blockchain = Blockchain.ETHEREUM,
            amount = ethValue,
            amountUsd = ethValue * 3000, // Approximate ETH price for now
            symbol = "ETH",
            fromAddress = from,
            toAddress = to,
            timestamp = (timeStamp.toLongOrNull() ?: 0L) * 1000,
            transactionHash = hash,
            isExchange = false,
        )
    }
}
