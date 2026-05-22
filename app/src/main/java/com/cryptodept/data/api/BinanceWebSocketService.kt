package com.cryptodept.data.api

import android.util.Log
import com.cryptodept.domain.model.MarketEvent
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Binance WebSocket Service for real-time price updates.
 * Lifecycle is managed externally by SocketLifecycleService.
 */
@Singleton
class BinanceWebSocketService
    @Inject
    constructor(
        @Named("PublicClient") private val okHttpClient: OkHttpClient,
        private val gson: Gson,
    ) {
        // Firebase pushes real-time price updates to all users centrally.
        // Per-device WebSocket connections are redundant and waste battery/CPU.
        // Re-enable by setting isDisabledByFirebase = false if Firebase is removed.
        val isDisabledByFirebase = true

        private val _priceUpdates =
            MutableSharedFlow<BinanceTickerResponse>(
                replay = 1,
                extraBufferCapacity = 10,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        val priceUpdates: SharedFlow<BinanceTickerResponse> = _priceUpdates.asSharedFlow()

        private val _marketEvents =
            MutableSharedFlow<MarketEvent>(
                extraBufferCapacity = 50,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        val marketEvents: SharedFlow<MarketEvent> = _marketEvents.asSharedFlow()

        private var webSocket: WebSocket? = null
        private var reconnectJob: Job? = null
        private var reconnectDelay = 1000L
        private val maxReconnectDelay = 30000L
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private val STREAM_URL =
            "wss://stream.binance.com:9443/stream?streams=btcusdt@ticker/ethusdt@ticker/xrpusdt@ticker/btcusdt@aggTrade/ethusdt@aggTrade/xrpusdt@aggTrade"

        fun connect() {
            if (isDisabledByFirebase) {
                Log.d("WebSocket", "${javaClass.simpleName} disabled — Firebase handles real-time data")
                return
            }
            if (webSocket != null) return
            startConnection()
        }

        private fun startConnection() {
            val request = Request.Builder().url(STREAM_URL).build()
            webSocket =
                okHttpClient.newWebSocket(
                    request,
                    object : WebSocketListener() {
                        override fun onOpen(
                            webSocket: WebSocket,
                            response: Response,
                        ) {
                            Log.d("CryptoDept_WS", "Binance WS connected")
                            reconnectDelay = 1000L
                        }

                        override fun onMessage(
                            webSocket: WebSocket,
                            text: String,
                        ) {
                            try {
                                val wrapper = gson.fromJson(text, BinanceStreamWrapper::class.java)
                                scope.launch {
                                    if (wrapper.stream.endsWith("@ticker")) {
                                        val ticker = gson.fromJson(gson.toJson(wrapper.data), BinanceTickerResponse::class.java)
                                        _priceUpdates.emit(ticker)
                                        _marketEvents.emit(
                                            MarketEvent.TickerUpdate(
                                                symbol = ticker.symbol,
                                                price = ticker.lastPrice.toDouble(),
                                                source = "Binance",
                                            )
                                        )
                                    } else if (wrapper.stream.endsWith("@aggTrade")) {
                                        val aggTrade = gson.fromJson(gson.toJson(wrapper.data), BinanceAggTradeResponse::class.java)
                                        val amountUsd = aggTrade.price.toDouble() * aggTrade.quantity.toDouble()
                                        if (amountUsd >= 100_000) { // Large trade threshold
                                            _marketEvents.emit(
                                                MarketEvent.LargeTrade(
                                                    symbol = aggTrade.symbol,
                                                    price = aggTrade.price.toDouble(),
                                                    quantity = aggTrade.quantity.toDouble(),
                                                    amountUsd = amountUsd,
                                                    side = if (aggTrade.isBuyerMaker) "SELL" else "BUY" // In Binance aggTrade, m=true means the maker was the buyer, so it's a sell order hitting the bid? No, m=true means the buyer is the market maker, which means the market taker sold. So m=true -> SELL, m=false -> BUY.
                                                )
                                            )
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("CryptoDept_WS", "Parse error: ${e.message}")
                            }
                        }

                        override fun onFailure(
                            webSocket: WebSocket,
                            t: Throwable,
                            response: Response?,
                        ) {
                            Log.e("CryptoDept_WS", "WS failure: ${t.message}")
                            this@BinanceWebSocketService.webSocket = null
                            // Reconnect logic is handled by SocketLifecycleService or locally if needed
                            // For now, let's keep local reconnect for small drops, 
                            // but external manager handles app-wide lifecycle.
                            scheduleReconnect()
                        }

                        override fun onClosed(
                            webSocket: WebSocket,
                            code: Int,
                            reason: String,
                        ) {
                            Log.d("CryptoDept_WS", "WS closed: $reason")
                            this@BinanceWebSocketService.webSocket = null
                        }
                    },
                )
        }

        private fun scheduleReconnect() {
            reconnectJob?.cancel()
            reconnectJob =
                scope.launch {
                    delay(reconnectDelay)
                    reconnectDelay = minOf(reconnectDelay * 2, maxReconnectDelay)
                    startConnection()
                }
        }

        fun disconnect() {
            reconnectJob?.cancel()
            webSocket?.close(1000, "User disconnect")
            webSocket = null
            Log.d("CryptoDept_WS", "Binance WS disconnected")
        }

        fun observeTickerStream(): kotlinx.coroutines.flow.Flow<BinanceTickerResponse> = priceUpdates
    }

data class BinanceStreamWrapper(
    val stream: String,
    val data: Any, // Change to Any to handle different data types
)

data class BinanceAggTradeResponse(
    @SerializedName("s") val symbol: String,
    @SerializedName("p") val price: String,
    @SerializedName("q") val quantity: String,
    @SerializedName("m") val isBuyerMaker: Boolean,
    @SerializedName("T") val timestamp: Long,
)

data class BinanceTickerResponse(
    @SerializedName("s") val symbol: String,
    @SerializedName("p") val priceChange: String,
    @SerializedName("P") val priceChangePercent: String,
    @SerializedName("w") val weightedAvgPrice: String,
    @SerializedName("x") val firstPrice: String,
    @SerializedName("c") val lastPrice: String,
    @SerializedName("Q") val lastQty: String,
    @SerializedName("b") val bidPrice: String,
    @SerializedName("B") val bidQty: String,
    @SerializedName("a") val askPrice: String,
    @SerializedName("A") val askQty: String,
    @SerializedName("o") val openPrice: String,
    @SerializedName("h") val highPrice: String,
    @SerializedName("l") val lowPrice: String,
    @SerializedName("v") val totalTradedBaseVolume: String,
    @SerializedName("q") val totalTradedQuoteVolume: String,
    @SerializedName("O") val openTime: Long,
    @SerializedName("C") val closeTime: Long,
    @SerializedName("F") val firstTradeId: Long,
    @SerializedName("L") val lastTradeId: Long,
    @SerializedName("n") val totalNumTrades: Long,
)
