package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import com.cryptodept.domain.model.HalvingCycle
import com.cryptodept.domain.usecase.HalvingDataProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SeasonalViewModel
    @Inject
    constructor(
        private val halvingProvider: HalvingDataProvider,
    ) : ViewModel() {
        private val _cycleInfo = MutableStateFlow(halvingProvider.getCurrentCycleInfo())
        val cycleInfo: StateFlow<HalvingCycle> = _cycleInfo.asStateFlow()

        fun refresh() {
            _cycleInfo.value = halvingProvider.getCurrentCycleInfo()
        }
    }
