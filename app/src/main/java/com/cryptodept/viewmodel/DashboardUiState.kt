package com.cryptodept.viewmodel

import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.model.LiquidationSummary
import com.cryptodept.domain.model.WhaleSignal
import com.cryptodept.domain.usecase.prediction.DailyAIPick

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(
        val prices: List<CoinPrice>,
        val isAdmin: Boolean,
        val whaleSignal: WhaleSignal = WhaleSignal.NEUTRAL,
        val dailyPick: DailyAIPick? = null,
        val shortPulse: String = "",
        val cloudWhaleAlerts: List<com.cryptodept.data.remote.model.CloudWhaleAlert> = emptyList(),
        val pricesLastUpdated: Long = 0L,
        val narrativeLastUpdated: Long = 0L,
        val fearGreedLastUpdated: Long = 0L,
        val whaleDataLastUpdated: Long = 0L,
        val liquidationSummary: LiquidationSummary? = null,
        val currentSession: com.cryptodept.util.MarketSession = com.cryptodept.util.MarketSession.OVERNIGHT,
        val sessionBrief: String? = null
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
