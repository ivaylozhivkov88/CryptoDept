package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import com.cryptodept.domain.manager.AchievementEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel
    @Inject
    constructor(
        val engine: AchievementEngine,
    ) : ViewModel()
