package com.cryptodept.viewmodel

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.update.AppUpdateRepository
import com.cryptodept.domain.update.AppUpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val updateRepository: AppUpdateRepository,
) : ViewModel() {
    
    val updateState: StateFlow<AppUpdateState> = updateRepository.updateState
    
    fun checkForUpdates() {
        viewModelScope.launch {
            updateRepository.checkForUpdates()
        }
    }
    
    fun startFlexibleUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ) {
        updateRepository.startFlexibleUpdate(activity, launcher)
    }
    
    fun startImmediateUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
    ) {
        updateRepository.startImmediateUpdate(activity, launcher)
    }
    
    fun completeUpdate() {
        updateRepository.completeUpdate()
    }
    
    fun dismissUpdate() {
        viewModelScope.launch {
            updateRepository.dismissUpdate()
        }
    }
}
