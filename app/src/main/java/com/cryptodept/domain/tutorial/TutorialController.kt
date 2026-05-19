package com.cryptodept.domain.tutorial

import com.cryptodept.data.datastore.PreferencesService
import com.cryptodept.domain.manager.AchievementEngine
import com.cryptodept.domain.model.AchievementCondition
import com.cryptodept.util.DemoModeProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton controller for the guided tour.
 * Survives configuration changes and navigation.
 */
@Singleton
class TutorialController @Inject constructor(
    private val preferencesService: PreferencesService,
    private val achievementEngine: AchievementEngine,
    private val demoMode: DemoModeProvider,
) {

    private val _state = MutableStateFlow(TutorialUiState())
    val state: StateFlow<TutorialUiState> = _state.asStateFlow()

    private val allSteps: List<TourStep> = TutorialContent.ALL_STEPS_FLAT

    fun shouldShowStartDialog(): Boolean {
        // Show start dialog only if:
        // - Tutorial is not completed
        // - It is not currently active
        return !preferencesService.isTutorialCompleted.value && !_state.value.isActive
    }

    fun promptToStartTutorial() {
        _state.value = _state.value.copy(showStartDialog = true)
    }

    fun dismissStartDialog() {
        _state.value = _state.value.copy(showStartDialog = false)
    }

    fun startTutorial() {
        demoMode.activate()
        _state.value = TutorialUiState(
            isActive = true,
            currentStepIndex = 0,
            currentStep = allSteps.firstOrNull(),
            showStartDialog = false
        )
    }

    fun nextStep() {
        val currentIdx = _state.value.currentStepIndex
        val nextIdx = currentIdx + 1

        if (nextIdx >= allSteps.size) {
            completeTutorial()
        } else {
            _state.value = _state.value.copy(
                currentStepIndex = nextIdx,
                currentStep = allSteps[nextIdx]
            )
        }
    }

    fun previousStep() {
        val currentIdx = _state.value.currentStepIndex
        if (currentIdx > 0) {
            _state.value = _state.value.copy(
                currentStepIndex = currentIdx - 1,
                currentStep = allSteps[currentIdx - 1]
            )
        }
    }

    fun requestSkip() {
        _state.value = _state.value.copy(showSkipConfirmation = true)
    }

    fun confirmSkip() {
        demoMode.deactivate()
        completeTutorial()
        _state.value = _state.value.copy(showSkipConfirmation = false)
    }

    fun cancelSkip() {
        _state.value = _state.value.copy(showSkipConfirmation = false)
    }

    fun completeTutorial() {
        demoMode.deactivate()
        // Set completed flag in preferences
        kotlinx.coroutines.MainScope().launch {
            preferencesService.setTutorialCompleted(true)
        }
        achievementEngine.triggerCondition(AchievementCondition.FIRST_BOOT)
        _state.value = TutorialUiState(
            isActive = false,
            showCompletionDialog = true
        )
    }

    fun dismissCompletionDialog() {
        _state.value = _state.value.copy(showCompletionDialog = false)
    }

    /**
     * Called from Settings → "Replay onboarding tour" button.
     */
    fun restartTutorial() {
        startTutorial()
    }

    companion object {
        const val KEY_TUTORIAL_COMPLETED = "tutorial_completed_v1"
    }
}
