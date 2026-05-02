package com.cryptodept.service

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.cryptodept.data.api.BinanceWebSocketManager
import com.cryptodept.data.api.KrakenWebSocketManager
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketLifecycleManager @Inject constructor(
    private val binanceWS: BinanceWebSocketManager,
    private val krakenWS: KrakenWebSocketManager
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var reconnectJob: Job? = null
    private var reconnectAttempt = 0
    private val MAX_RECONNECT_ATTEMPTS = 10
    private val INITIAL_DELAY_MS = 1_000L

    fun init() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Log.d("CryptoDept_Lifecycle", "SocketLifecycleManager initialized")
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d("CryptoDept_Lifecycle", "App moved to foreground - connecting WebSockets")
        connectWithRetry()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d("CryptoDept_Lifecycle", "App moved to background - disconnecting WebSockets")
        disconnect()
    }

    private fun connectWithRetry() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            try {
                binanceWS.connect()
                krakenWS.connect()
                reconnectAttempt = 0
                Log.d("CryptoDept_Lifecycle", "WebSockets connected successfully")
            } catch (e: Exception) {
                if (reconnectAttempt < MAX_RECONNECT_ATTEMPTS) {
                    val delayMs = INITIAL_DELAY_MS * (1 shl reconnectAttempt)  // Exponential backoff
                    reconnectAttempt++
                    Log.w("CryptoDept_Lifecycle", "Connect attempt $reconnectAttempt failed, retrying in ${delayMs}ms", e)
                    delay(delayMs)
                    connectWithRetry()
                } else {
                    Log.e("CryptoDept_Lifecycle", "Max reconnect attempts reached", e)
                }
            }
        }
    }

    private fun disconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempt = 0
        try {
            binanceWS.disconnect()
            krakenWS.disconnect()
            Log.d("CryptoDept_Lifecycle", "WebSockets disconnected")
        } catch (e: Exception) {
            Log.e("CryptoDept_Lifecycle", "Error disconnecting WebSockets", e)
        }
    }

    fun cleanup() {
        scope.cancel()
        Log.d("CryptoDept_Lifecycle", "SocketLifecycleManager cleanup completed")
    }
}
