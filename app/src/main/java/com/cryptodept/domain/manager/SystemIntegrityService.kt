package com.cryptodept.domain.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

data class IntegrityLog(
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val isAnomaly: Boolean = false
)

@Singleton
class SystemIntegrityService @Inject constructor() {
    private val _logs = MutableStateFlow<List<IntegrityLog>>(emptyList())
    val logs: StateFlow<List<IntegrityLog>> = _logs.asStateFlow()

    private val logBuffer = ConcurrentLinkedQueue<IntegrityLog>()
    private val MAX_LOGS = 5

    fun addLog(message: String, isAnomaly: Boolean = false) {
        val log = IntegrityLog(message = message, isAnomaly = isAnomaly)
        logBuffer.add(log)
        if (logBuffer.size > MAX_LOGS) {
            logBuffer.poll()
        }
        _logs.value = logBuffer.toList().reversed()
    }
}
