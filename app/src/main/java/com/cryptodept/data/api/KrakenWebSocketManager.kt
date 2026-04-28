package com.cryptodept.data.api

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class KrakenWebSocketManager @Inject constructor(
    @Named("PublicClient") private val client: OkHttpClient,
    private val gson: Gson
) {
    fun observeTickerStream(): Flow<Pair<String, Double>> = callbackFlow {
        val request = Request.Builder().url("wss://ws.kraken.com/v2").build()
        
        val listener = object : WebSocketListener() {
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
                            trySend(coinId to lastPrice)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CryptoDept_WS", "Kraken Parse Error: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(t)
            }
        }

        val webSocket = client.newWebSocket(request, listener)

        awaitClose {
            webSocket.close(1000, "Flow closed")
        }
    }
}
