package com.cryptodept.ui.screensaver

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ScreensaverType {
    BLOOMBERG_WALL,
    MATRIX_RAIN,
    HEATMAP,
}

@Singleton
class ScreensaverCycleService
    @Inject
    constructor() {
        private val _currentScreensaver = MutableStateFlow(ScreensaverType.MATRIX_RAIN)
        val currentScreensaver = _currentScreensaver.asStateFlow()

        private var cycleJob: Job? = null

        fun startCycling() {
            if (cycleJob?.isActive == true) return
            
            // Stay on Matrix Rain as requested for elite feel
            _currentScreensaver.value = ScreensaverType.MATRIX_RAIN
        }

        fun stopCycling() {
            cycleJob?.cancel()
            cycleJob = null
        }
    }
