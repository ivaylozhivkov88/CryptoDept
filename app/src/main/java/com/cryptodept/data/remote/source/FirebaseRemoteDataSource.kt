package com.cryptodept.data.remote.source

import com.cryptodept.data.remote.model.CloudMacroBriefing
import com.cryptodept.data.remote.model.CloudTerminalState
import com.cryptodept.data.remote.model.CloudWhaleAlert
import com.cryptodept.data.remote.model.CloudMarketData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRemoteDataSource @Inject constructor(
    private val database: FirebaseDatabase
) {
    /**
     * Слуша за макро данни и глобален статус (пести трафик).
     */
    fun getGlobalState(): Flow<CloudMacroBriefing?> = callbackFlow {
        val ref = database.getReference("terminal_state/macroBriefing")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(CloudMacroBriefing::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                // SILENT FAIL: Don't crash the app if no permissions yet
                trySend(null)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getAiNarrative(): Flow<String?> = callbackFlow {
        val ref = database.getReference("terminal_state/aiNarrative")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(String::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getWhaleAlerts(): Flow<List<CloudWhaleAlert>> = callbackFlow {
        val ref = database.getReference("terminal_state/whaleAlerts")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val alerts = mutableListOf<CloudWhaleAlert>()
                snapshot.children.forEach { child ->
                    child.getValue(CloudWhaleAlert::class.java)?.let { alerts.add(it) }
                }
                trySend(alerts)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getAgentStatuses(): Flow<Map<String, String>> = callbackFlow {
        val ref = database.getReference("terminal_state/agentStatuses")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, String>()
                snapshot.children.forEach { child ->
                    val key = child.key ?: return@forEach
                    val value = child.getValue(String::class.java) ?: return@forEach
                    map[key] = value
                }
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyMap())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getAgentReports(): Flow<Map<String, String>> = callbackFlow {
        val ref = database.getReference("terminal_state/agentReports")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val map = mutableMapOf<String, String>()
                snapshot.children.forEach { child ->
                    val key = child.key ?: return@forEach
                    val value = child.getValue(String::class.java) ?: return@forEach
                    map[key] = value
                }
                trySend(map)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(emptyMap())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getUpdateTimestampFlow(): Flow<Long> = callbackFlow {
        val ref = database.getReference("terminal_state/lastUpdateTimestamp")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(Long::class.java) ?: 0L)
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(0L)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Legacy support method that reconstructs the state from granular flows.
     */
    fun getTerminalState(): Flow<CloudTerminalState?> {
        val coreFlow = combine(
            getGlobalState(),
            getWhaleAlerts(),
            getAgentStatuses(),
            getAgentReports()
        ) { macro, whale, statuses, reports ->
            CloudTerminalState(
                macroBriefing = macro,
                whaleAlerts = whale,
                agentStatuses = statuses,
                agentReports = reports,
                marketData = emptyMap()
            )
        }

        return combine(coreFlow, getAiNarrative(), getUpdateTimestampFlow()) { state, narrative, ts ->
            state.copy(
                aiNarrative = narrative ?: "",
                lastUpdateTimestamp = ts
            )
        }
    }

    /**
     * Слуша само за конкретна монета (спестява трафик).
     */
    fun getCoinData(coinId: String): Flow<CloudMarketData?> = callbackFlow {
        val ref = database.getReference("terminal_state/marketData/$coinId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(CloudMarketData::class.java))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * NEW: Слуша за сървърен статус на абонамента (Agent Auditor).
     */
    fun getUserTier(uid: String): Flow<String?> = callbackFlow {
        val ref = database.getReference("users/$uid/access_tier")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tier = snapshot.getValue(String::class.java)
                trySend(tier)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Бърза проверка на последното обновяване без теглене на целия стейт.
     */
    suspend fun getLastUpdateTimestamp(): Long {
        return try {
            val snapshot = com.google.android.gms.tasks.Tasks.await(
                database.getReference("terminal_state/lastUpdateTimestamp").get()
            )
            snapshot.getValue(Long::class.java) ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Fetch cloud-calculated predictions to save API calls.
     */
    suspend fun getCloudPrediction(coinId: String): Map<String, Any>? {
        return try {
            val snapshot = com.google.android.gms.tasks.Tasks.await(
                database.getReference("terminal_state/cloudPredictions/$coinId").get()
            )
            @Suppress("UNCHECKED_CAST")
            snapshot.value as? Map<String, Any>
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Слуша за промени в любимите монети на конкретен потребител.
     */
    fun getUserWatchlist(uid: String): Flow<List<String>> = callbackFlow {
        val ref = database.getReference("users/$uid/watchlist")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<String>()
                snapshot.children.forEach { child ->
                    child.key?.let { list.add(it) }
                }
                trySend(list)
            }
            override fun onCancelled(error: DatabaseError) { trySend(emptyList()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Записва или изтрива монета от списъка на потребителя в облака.
     */
    suspend fun setUserWatchlist(uid: String, coinId: String, isTracked: Boolean) {
        kotlin.runCatching {
            val ref = database.getReference("users/$uid/watchlist/$coinId")
            if (isTracked) {
                com.google.android.gms.tasks.Tasks.await(ref.setValue(true))
            } else {
                com.google.android.gms.tasks.Tasks.await(ref.removeValue())
            }
        }.onFailure { e ->
            android.util.Log.e("FirebaseRemote", "FATAL_SYNC_ERROR for $coinId: ${e.message}")
        }
    }

    /**
     * GDPR COMPLIANCE: Erases all cloud-stored metadata for the given user.
     */
    suspend fun deleteUserData(uid: String): Result<Unit> {
        return try {
            val ref = database.getReference("users/$uid")
            com.google.android.gms.tasks.Tasks.await(ref.removeValue())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
