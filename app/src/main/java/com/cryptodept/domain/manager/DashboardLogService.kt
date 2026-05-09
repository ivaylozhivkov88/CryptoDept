package com.cryptodept.domain.manager

import com.cryptodept.domain.model.EventType
import com.cryptodept.domain.model.SystemEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DashboardLogService
    @Inject
    constructor() {
        private val maxLogs = 50
        private val _events = MutableStateFlow<List<SystemEvent>>(emptyList())
        val events: StateFlow<List<SystemEvent>> = _events.asStateFlow()

        init {
            // Initial boot log
            addEvent(EventType.SYSTEM_STATUS, "TERMINAL CORE V3.0 ONLINE. ALL SYSTEMS NOMINAL.")
        }

        fun addEvent(
            type: EventType,
            message: String,
        ) {
            val newEvent = SystemEvent(type = type, message = message)
            val currentList = _events.value.toMutableList()
            currentList.add(0, newEvent) // Add to top

            if (currentList.size > maxLogs) {
                _events.value = currentList.take(maxLogs)
            } else {
                _events.value = currentList
            }
        }

        fun clearLogs() {
            _events.value = emptyList()
        }
    }
