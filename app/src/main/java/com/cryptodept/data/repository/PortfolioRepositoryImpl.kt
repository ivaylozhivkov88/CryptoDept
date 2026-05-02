package com.cryptodept.data.repository

import com.cryptodept.data.db.PortfolioDao
import com.cryptodept.data.db.PortfolioEntity
import com.cryptodept.domain.model.*
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepositoryImpl @Inject constructor(
    private val portfolioDao: PortfolioDao,
    private val cryptoRepository: CryptoRepository
) : PortfolioRepository {

    override fun getPortfolioEntries(): Flow<List<PortfolioEntry>> {
        return portfolioDao.getAllEntries().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPortfolioSummary(): Flow<PortfolioSummary> {
        return combine(
            getPortfolioEntries(),
            cryptoRepository.getTrackedCoinPrices()
        ) { entries, prices ->
            val priceMap = prices.associateBy { it.id }
            
            val entriesWithPrice = entries.map { entry ->
                val currentPrice = priceMap[entry.coinId]?.currentPrice ?: 0.0
                val currentValue = currentPrice * entry.quantity
                val costValue = entry.averageEntryPrice * entry.quantity
                val pnlUsd = currentValue - costValue
                val pnlPercent = if (costValue > 0) (pnlUsd / costValue) * 100 else 0.0
                
                PortfolioEntryWithCurrentPrice(
                    entry = entry,
                    currentPrice = currentPrice,
                    pnlUsd = pnlUsd,
                    pnlPercent = pnlPercent,
                    currentValueUsd = currentValue
                )
            }

            val totalValue = entriesWithPrice.sumOf { it.currentValueUsd }
            val totalCost = entriesWithPrice.sumOf { it.entry.averageEntryPrice * it.entry.quantity }
            val totalPnlUsd = totalValue - totalCost
            val totalPnlPercent = if (totalCost > 0) (totalPnlUsd / totalCost) * 100 else 0.0

            PortfolioSummary(
                totalValueUsd = totalValue,
                totalCostUsd = totalCost,
                totalPnlUsd = totalPnlUsd,
                totalPnlPercent = totalPnlPercent,
                entries = entriesWithPrice
            )
        }
    }

    override suspend fun addPosition(entry: PortfolioEntry) {
        portfolioDao.insertEntry(PortfolioEntity.fromDomain(entry))
    }

    override suspend fun removePosition(id: String) {
        portfolioDao.deleteEntryById(id)
    }
}
