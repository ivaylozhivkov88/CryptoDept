package com.cryptodept.data.repository

import com.cryptodept.data.api.DefiLlamaApi
import com.cryptodept.domain.model.DeFiProtocol
import com.cryptodept.domain.model.DeFiYieldOpportunity
import com.cryptodept.domain.repository.DeFiRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeFiRepositoryImpl @Inject constructor(
    private val defiLlamaApi: DefiLlamaApi
) : DeFiRepository {

    override suspend fun getTopProtocols(): Result<List<DeFiProtocol>> = try {
        val response = defiLlamaApi.getProtocols()
        val protocols = response.take(20).map { dto ->
            DeFiProtocol(
                id = dto.id,
                name = dto.name,
                symbol = dto.symbol,
                url = dto.url,
                description = dto.description ?: "",
                logo = dto.logo,
                tvl = dto.tvl,
                tvlChange1h = dto.tvlChange1h ?: 0.0,
                tvlChange1d = dto.tvlChange1d ?: 0.0,
                tvlChange7d = dto.tvlChange7d ?: 0.0,
                chain = dto.chain ?: "Multi",
                category = dto.category ?: "Unknown"
            )
        }
        Result.success(protocols)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getTopYields(): Result<List<DeFiYieldOpportunity>> = try {
        val response = defiLlamaApi.getYields()
        val pools = response.data
            .filter { it.tvlUsd > 1_000_000 }
            .sortedByDescending { it.apy }
            .take(20)
            .map { dto ->
                DeFiYieldOpportunity(
                    protocol = dto.protocol,
                    symbol = dto.symbol,
                    tvl = dto.tvlUsd,
                    apy = dto.apy,
                    chain = dto.chain
                )
            }
        Result.success(pools)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
