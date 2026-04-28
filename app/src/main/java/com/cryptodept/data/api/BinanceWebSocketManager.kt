package com.cryptodept.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class BinanceWebSocketManager @Inject constructor(
    @Named("PublicClient") private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private val _priceUpdates = MutableSharedFlow<BinanceTickerResponse>(
        replay = 1,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val priceUpdates: SharedFlow<BinanceTickerResponse> = _priceUpdates.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectDelay = 1000L
    private val maxReconnectDelay = 30000L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val STREAM_URL = "wss://stream.binance.com:9443/stream?streams=btcusdt@ticker/ethusdt@ticker/xrpusdt@ticker"

    fun connect() {
        if (webSocket != null) return
        startConnection()
    }

    private fun startConnection() {
        val request = Request.Builder().url(STREAM_URL).build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("CryptoDept_WS", "Binance WS connected")
                reconnectDelay = 1000L
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val wrapper = gson.fromJson(text, BinanceStreamWrapper::class.java)
                    scope.launch {
                        _priceUpdates.emit(wrapper.data)
                    }
                } catch (e: Exception) {
                    Log.e("CryptoDept_WS", "Parse error: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("CryptoDept_WS", "WS failure: ${t.message}. Reconnecting in ${reconnectDelay}ms")
                this@BinanceWebSocketManager.webSocket = null
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("CryptoDept_WS", "WS closed: $reason")
                this@BinanceWebSocketManager.webSocket = null
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(reconnectDelay)
            reconnectDelay = minOf(reconnectDelay * 2, maxReconnectDelay)
            startConnection()
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnect")
        webSocket = null
    }

    // Keep original method for compatibility if needed, but updated to use SharedFlow
    fun observeTickerStream(): kotlinx.coroutines.flow.Flow<BinanceTickerResponse> = priceUpdates
}

data class BinanceStreamWrapper(
    val stream: String,
    val data: BinanceTickerResponse
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
    @SerializedName("n") val totalNumTrades: Long
)
