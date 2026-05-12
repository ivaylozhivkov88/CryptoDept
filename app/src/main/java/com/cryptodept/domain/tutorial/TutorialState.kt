package com.cryptodept.domain.tutorial

/**
 * Tutorial UI state — observed from screens and overlay.
 */
data class TutorialUiState(
    val isActive: Boolean = false,
    val currentStepIndex: Int = 0,
    val currentStep: TourStep? = null,
    val totalSteps: Int = TutorialContent.TOTAL_STEPS,
    val showStartDialog: Boolean = false,
    val showCompletionDialog: Boolean = false,
    val showSkipConfirmation: Boolean = false
)
