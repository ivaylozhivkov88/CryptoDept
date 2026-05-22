package com.cryptodept.data.remote.source

import com.cryptodept.data.remote.model.CloudTerminalState
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseRemoteDataSource @Inject constructor(
    private val database: FirebaseDatabase
) {
    /**
     * Слуша за промени в целия терминален стейт в реално време.
     */
    fun getTerminalState(): Flow<CloudTerminalState?> = callbackFlow {
        val ref = database.getReference("terminal_state")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = snapshot.getValue(CloudTerminalState::class.java)
                trySend(state)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Слуша само за конкретна монета (спестява трафик).
     */
    fun getCoinData(coinId: String): Flow<com.cryptodept.data.remote.model.CloudMarketData?> = callbackFlow {
        val ref = database.getReference("terminal_state/marketData/$coinId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.getValue(com.cryptodept.data.remote.model.CloudMarketData::class.java))
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
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
}
