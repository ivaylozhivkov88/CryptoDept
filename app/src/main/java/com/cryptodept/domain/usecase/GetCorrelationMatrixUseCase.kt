package com.cryptodept.domain.usecase

import android.util.Log
import com.cryptodept.domain.model.CorrelationMatrix
import com.cryptodept.domain.repository.CryptoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GetCorrelationMatrixUseCase
    @Inject
    constructor(
        private val cryptoRepository: CryptoRepository,
        private val correlationEngine: CorrelationEngine,
    ) {
        suspend fun execute(
            symbols: List<String>,
            days: Int = 30,
        ): Result<CorrelationMatrix> =
            withContext(Dispatchers.IO) {
                try {
                    // 1. Fetch data for all symbols in parallel
                    val dataResults =
                        symbols
                            .map { symbol ->
                                async {
                                    val id = normalizeId(symbol)
                                    val data = cryptoRepository.getOHLCData(id, days)
                                    symbol to data.map { it.timestamp to it.close }
                                }
                            }.awaitAll()
                            .toMap()

                    // 2. Build matrix
                    val matrix = mutableListOf<List<Double>>()

                    for (i in symbols.indices) {
                        val row = mutableListOf<Double>()
                        val symbol1 = symbols[i]
                        val data1 = dataResults[symbol1] ?: emptyList()

                        for (j in symbols.indices) {
                            val symbol2 = symbols[j]
                            val data2 = dataResults[symbol2] ?: emptyList()

                            if (i == j) {
                                row.add(1.0)
                            } else {
                                val (aligned1, aligned2) = correlationEngine.alignPrices(data1, data2)
                                val correlation = correlationEngine.calculatePearson(aligned1, aligned2)
                                row.add(correlation)
                            }
                        }
                        matrix.add(row)
                    }

                    Result.success(CorrelationMatrix(symbols, matrix))
                } catch (e: Exception) {
                    Log.e("CorrelationUseCase", "Failed to calculate matrix: ${e.message}")
                    Result.failure(e)
                }
            }

        private fun normalizeId(symbol: String): String =
            when (symbol.lowercase()) {
                "btc" -> "bitcoin"
                "eth" -> "ethereum"
                "xrp" -> "ripple"
                "sol" -> "solana"
                "ada" -> "cardano"
                "dot" -> "polkadot"
                "ltc" -> "litecoin"
                "link" -> "chainlink"
                "matic" -> "matic-network"
                "avax" -> "avalanche-2"
                "trx" -> "tron"
                "xlm" -> "stellar"
                "atom" -> "cosmos"
                "shib" -> "shiba-inu"
                "doge" -> "dogecoin"
                else -> symbol.lowercase()
            }
    }
