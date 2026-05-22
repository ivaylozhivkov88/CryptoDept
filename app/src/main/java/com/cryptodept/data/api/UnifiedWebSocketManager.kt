package com.cryptodept.data.api

import com.cryptodept.domain.model.MarketEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedWebSocketManager @Inject constructor(
    private val binanceWS: BinanceWebSocketService,
    private val krakenWS: KrakenWebSocketService
) {
    val marketEvents: Flow<MarketEvent> = merge(
        binanceWS.marketEvents,
        krakenWS.marketEvents
    )

    fun connectAll() {
        if (!binanceWS.isDisabledByFirebase) {
            binanceWS.connect()
        }
        if (!krakenWS.isDisabledByFirebase) {
            krakenWS.connect()
        }
    }

    fun disconnectAll() {
        binanceWS.disconnect()
        krakenWS.disconnect()
    }
}
