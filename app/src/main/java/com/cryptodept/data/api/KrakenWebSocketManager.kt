package com.cryptodept.data.api

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class KrakenWebSocketManager @Inject constructor(
    @Named("PublicClient") private val client: OkHttpClient,
    private val gson: Gson
) {
    private val _priceUpdates = MutableSharedFlow<Pair<String, Double>>(
        replay = 1,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val priceUpdates: SharedFlow<Pair<String, Double>> = _priceUpdates.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectDelay = 1000L
    private val maxReconnectDelay = 30000L
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val KRAKEN_WS_URL = "wss://ws.kraken.com/v2"

    fun connect() {
        if (webSocket != null) return
        startConnection()
    }

    private fun startConnection() {
        val request = Request.Builder().url(KRAKEN_WS_URL).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val subscribeMsg = """
                    {
                      "method": "subscribe",
                      "params": {
                        "channel": "ticker",
                        "symbol": ["BTC/USD", "ETH/USD", "XRP/USD"]
                      }
                    }
                """.trimIndent()
                webSocket.send(subscribeMsg)
                Log.d("CryptoDept_WS", "Kraken Connected")
                reconnectDelay = 1000L
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val map = gson.fromJson(text, Map::class.java)
                    if (map["channel"] == "ticker") {
                        val dataList = map["data"] as? List<*>
                        val data = dataList?.firstOrNull() as? Map<*, *>
                        val symbol = data?.get("symbol") as? String
                        val lastPrice = (data?.get("last") as? Double) ?: 0.0
                        
                        val coinId = when(symbol) {
                            "BTC/USD" -> "bitcoin"
                            "ETH/USD" -> "ethereum"
                            "XRP/USD" -> "ripple"
                            else -> null
                        }
                        
                        if (coinId != null && lastPrice > 0) {
                            scope.launch {
                                _priceUpdates.emit(coinId to lastPrice)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CryptoDept_WS", "Kraken Parse Error: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("CryptoDept_WS", "Kraken failure: ${t.message}")
                this@KrakenWebSocketManager.webSocket = null
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                this@KrakenWebSocketManager.webSocket = null
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

    fun observeTickerStream(): Flow<Pair<String, Double>> = priceUpdates
}
