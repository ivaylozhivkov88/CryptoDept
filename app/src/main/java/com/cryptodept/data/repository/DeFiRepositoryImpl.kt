package com.cryptodept.data.repository

import com.cryptodept.data.api.defillama.DefiLlamaService
import com.cryptodept.domain.model.DeFiProtocol
import com.cryptodept.domain.model.DeFiYieldOpportunity
import com.cryptodept.domain.repository.DeFiRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeFiRepositoryImpl @Inject constructor(
    private val defiLlamaService: DefiLlamaService
) : DeFiRepository {

    override suspend fun getTopProtocols(): Result<List<DeFiProtocol>> = try {
        val response = defiLlamaService.getAllProtocols()
        val protocols = response.take(30).map { dto ->
            DeFiProtocol(
                id = dto.id,
                name = dto.name,
                symbol = dto.symbol ?: "",
                url = "", 
                description = "",
                logo = "",
                tvl = dto.tvl,
                tvlChange1h = 0.0,
                tvlChange1d = dto.change_1d ?: 0.0,
                tvlChange7d = 0.0,
                chain = dto.chains?.firstOrNull() ?: "Multi",
                category = dto.category ?: "Unknown"
            )
        }
        Result.success(protocols)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getTopYields(): Result<List<DeFiYieldOpportunity>> = try {
        val response = defiLlamaService.getYieldPools()
        val pools = response.data
            .filter { (it.apy ?: 0.0) > 0 && it.tvlUsd > 1_000_000 }
            .sortedByDescending { it.apy ?: 0.0 }
            .take(50)
            .map { dto ->
                DeFiYieldOpportunity(
                    protocol = dto.project,
                    symbol = dto.symbol,
                    tvl = dto.tvlUsd,
                    apy = dto.apy ?: 0.0,
                    chain = dto.chain
                )
            }
        Result.success(pools)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
