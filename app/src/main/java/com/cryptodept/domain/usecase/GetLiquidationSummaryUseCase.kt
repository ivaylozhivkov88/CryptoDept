package com.cryptodept.domain.usecase

import com.cryptodept.domain.model.LiquidationSummary
import com.cryptodept.domain.repository.CoinGlassRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetLiquidationSummaryUseCase @Inject constructor(
    private val repository: CoinGlassRepository
) {
    suspend operator fun invoke(
        symbol: String,
        currentPrice: Double
    ): Result<LiquidationSummary> {
        val cgSymbol = when (symbol.uppercase()) {
            "BITCOIN" -> "BTC"
            "ETHEREUM" -> "ETH"
            "SOLANA" -> "SOL"
            "BINANCECOIN" -> "BNB"
            "XRP" -> "XRP"
            "DOGECOIN" -> "DOGE"
            "CARDANO" -> "ADA"
            "LITECOIN" -> "LTC"
            else -> symbol.uppercase()
        }

        return repository.getLiquidationMap(cgSymbol).map { response ->
            val data = response.data
            
            val (totalLong, nearestLong, totalShort, nearestShort) = when {
                data?.chartData != null -> {
                    val chart = data.chartData
                    val prices = chart.pricelevels
                    val longs = chart.longLiquidations
                    val shorts = chart.shortLiquidations
                    
                    var tLong = 0.0
                    var nLong = 0.0
                    var tShort = 0.0
                    var nShort = 0.0
                    
                    prices.forEachIndexed { index, price ->
                        val l = longs.getOrNull(index) ?: 0.0
                        val s = shorts.getOrNull(index) ?: 0.0
                        
                        if (price < currentPrice) {
                            tLong += l
                            if (price > nLong) nLong = price
                        } else if (price > currentPrice) {
                            tShort += s
                            if (nShort == 0.0 || price < nShort) nShort = price
                        }
                    }
                    arrayOf(tLong, nLong, tShort, nShort)
                }
                !data?.liqList.isNullOrEmpty() -> {
                    val list = data!!.liqList!!
                    val longLevels = list.filter { (it.direction == "long" || it.direction == "buy") && it.price < currentPrice }
                        .sortedByDescending { it.price }
                    val shortLevels = list.filter { (it.direction == "short" || it.direction == "sell") && it.price > currentPrice }
                        .sortedBy { it.price }
                    
                    arrayOf(
                        longLevels.sumOf { it.liqSize },
                        longLevels.firstOrNull()?.price ?: 0.0,
                        shortLevels.sumOf { it.liqSize },
                        shortLevels.firstOrNull()?.price ?: 0.0
                    )
                }
                else -> arrayOf(0.0, 0.0, 0.0, 0.0)
            }
            
            val tLong = totalLong as Double
            val nLong = nearestLong as Double
            val tShort = totalShort as Double
            val nShort = nearestShort as Double

            val totalLiquidity = tLong + tShort
            val longDominance = if (totalLiquidity > 0) (tLong / totalLiquidity).toFloat() else 0.5f
            
            LiquidationSummary(
                symbol = symbol,
                currentPrice = currentPrice,
                nearestLongLevel = nLong,
                totalLongLiquidity = tLong,
                nearestShortLevel = nShort,
                totalShortLiquidity = tShort,
                longDominance = longDominance
            )
        }
    }
}
