package com.cryptodept.service

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.cryptodept.data.api.UnifiedWebSocketManager
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketLifecycleService
    @Inject
    constructor(
        private val wsManager: UnifiedWebSocketManager,
    ) : DefaultLifecycleObserver {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private var reconnectJob: Job? = null
        private var reconnectAttempt = 0
        private val MAX_RECONNECT_ATTEMPTS = 10
        private val INITIAL_DELAY_MS = 1_000L

        fun init() {
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            Log.d("CryptoDept_Lifecycle", "SocketLifecycleService initialized and observing process lifecycle")
        }

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            Log.d("CryptoDept_Lifecycle", "App ENTERED FOREGROUND - Connecting WebSockets")
            connectWithRetry()
        }

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            Log.d("CryptoDept_Lifecycle", "App ENTERED BACKGROUND - Terminating WebSockets INSTANTLY")
            disconnect()
        }

        private fun connectWithRetry() {
            reconnectJob?.cancel()
            reconnectJob =
                scope.launch {
                    try {
                        wsManager.connectAll()
                        reconnectAttempt = 0
                        Log.d("CryptoDept_Lifecycle", "All WebSockets connected")
                    } catch (e: Exception) {
                        Log.e("CryptoDept_Lifecycle", "Connection failed: ${e.message}")
                        handleReconnect()
                    }
                }
        }

        private suspend fun handleReconnect() {
            if (reconnectAttempt < MAX_RECONNECT_ATTEMPTS) {
                val delayMs = INITIAL_DELAY_MS * (1 shl reconnectAttempt)
                reconnectAttempt++
                Log.w("CryptoDept_Lifecycle", "Connect attempt $reconnectAttempt failed, retrying in ${delayMs}ms")
                delay(delayMs)
                connectWithRetry()
            } else {
                Log.e("CryptoDept_Lifecycle", "Max reconnect attempts reached. Waiting for next lifecycle event.")
            }
        }

        private fun disconnect() {
            reconnectJob?.cancel()
            reconnectJob = null
            reconnectAttempt = 0
            try {
                wsManager.disconnectAll()
                Log.d("CryptoDept_Lifecycle", "All WebSockets disconnected successfully")
            } catch (e: Exception) {
                Log.e("CryptoDept_Lifecycle", "Error during WebSocket disconnect", e)
            }
        }

        fun cleanup() {
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
            disconnect()
            scope.cancel()
        }
    }
