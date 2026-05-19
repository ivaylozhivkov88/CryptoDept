package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.content.PromptTemplates
import com.cryptodept.domain.model.AudienceProfile
import com.cryptodept.domain.repository.AIProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContentStudioViewModel
    @Inject
    constructor(
        private val aiProvider: AIProvider,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ContentStudioUiState())
        val uiState: StateFlow<ContentStudioUiState> = _uiState.asStateFlow()

        private val _generatedPrompt = MutableStateFlow("")
        val generatedPrompt: StateFlow<String> = _generatedPrompt.asStateFlow()

        private val _aiResponse = MutableStateFlow("")
        val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

        @Suppress("UNCHECKED_CAST")
    fun generatePrompt(
        template: ContentTemplate,
        audience: AudienceProfile,
        params: Map<String, Any>,
    ) {
            val prompt = PromptTemplates.buildPrompt(template, audience, params)
            _generatedPrompt.value = prompt
        }

        fun sendToAi() {
            val prompt = _generatedPrompt.value
            if (prompt.isBlank()) return

            _uiState.update { it.copy(isLoading = true, error = null) }
            _aiResponse.value = ""

            viewModelScope.launch {
                try {
                    aiProvider.sendMessage(prompt).collectLatest { chunk ->
                        _aiResponse.value += chunk
                    }
                    _uiState.update { it.copy(isLoading = false) }
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "AI Error") }
                }
            }
        }

        fun sendPromptToAiCoach(prompt: String) {
            if (prompt.isBlank()) return
            _uiState.update { it.copy(pendingNavigationToAiCoach = prompt) }
        }

        fun navigationConsumed() {
            _uiState.update { it.copy(pendingNavigationToAiCoach = null) }
        }
    }

enum class ContentTemplate(
    val label: String,
) {
    DAILY_RECAP("Daily Market Recap"),
    VIDEO_PROMPT("Video Prompt (Visual)"),
    THUMBNAIL("YouTube Thumbnail"),
    FACEBOOK_POST("Facebook Post"),
    WHALE_NARRATOR("Whale Alert Script"),
    WHAT_IF("What If Scenario"),
    COMPARISON("Coin Comparison"),
    NEWSLETTER("Newsletter Digest"),
}

data class ContentStudioUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingNavigationToAiCoach: String? = null,
)
