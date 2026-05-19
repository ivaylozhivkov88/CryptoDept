package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import com.cryptodept.domain.tier.TierAccessManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ToolsHubViewModel @Inject constructor(
    val tierAccessManager: TierAccessManager
) : ViewModel()
