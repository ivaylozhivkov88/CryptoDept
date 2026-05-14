package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.algo.TreemapItem
import com.cryptodept.domain.model.AudienceProfile
import com.cryptodept.domain.model.NetworkHealth
import com.cryptodept.domain.model.WhaleTransaction
import com.cryptodept.domain.repository.AIProvider
import com.cryptodept.domain.usecase.*
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
        private val dailyRecapBuilder: DailyRecapPromptBuilder,
        private val videoPromptBuilder: VideoPromptBuilder,
        private val thumbnailPromptBuilder: ThumbnailPromptBuilder,
        private val facebookPostBuilder: FacebookPostPromptBuilder,
        private val whaleNarratorBuilder: WhaleNarratorPromptBuilder,
        private val speculativeBuilder: SpeculativeScenarioPromptBuilder,
        private val comparisonBuilder: CoinComparisonPromptBuilder,
        private val newsletterBuilder: NewsletterPromptBuilder,
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
            val prompt =
                when (template) {
                    ContentTemplate.DAILY_RECAP -> {
                        dailyRecapBuilder.build(
                            audience = audience,
                            health = params["health"] as? NetworkHealth ?: NetworkHealth("N/A", "N/A", "N/A", 50, "Neutral"),
                            topMovers = params["topMovers"] as? List<TreemapItem> ?: emptyList(),
                            recentNews = params["recentNews"] as? List<String> ?: emptyList(),
                        )
                    }
                    ContentTemplate.VIDEO_PROMPT -> {
                        videoPromptBuilder.build(
                            sentimentScore = params["sentiment"] as? Int ?: 50,
                            mainAsset = params["asset"] as? String ?: "BTC",
                        )
                    }
                    ContentTemplate.THUMBNAIL -> {
                        thumbnailPromptBuilder.build(
                            headline = params["headline"] as? String ?: "КРИПТО ШОК",
                            mainCoin = params["asset"] as? String ?: "BTC",
                            isBullish = params["isBullish"] as? Boolean ?: true,
                        )
                    }
                    ContentTemplate.FACEBOOK_POST -> {
                        facebookPostBuilder.build(
                            topic = params["topic"] as? String ?: "Market Update",
                            dataContext = params["context"] as? String ?: "",
                        )
                    }
                    ContentTemplate.WHALE_NARRATOR -> {
                        val tx = params["tx"] as? WhaleTransaction
                        if (tx != null) whaleNarratorBuilder.build(tx) else "No transaction data."
                    }
                    ContentTemplate.WHAT_IF -> {
                        speculativeBuilder.build(
                            asset = params["asset"] as? String ?: "BTC",
                            targetPrice = params["price"] as? String ?: "$100,000",
                        )
                    }
                    ContentTemplate.COMPARISON -> {
                        comparisonBuilder.build(
                            coinA = params["coinA"] as? String ?: "ETH",
                            coinB = params["coinB"] as? String ?: "SOL",
                        )
                    }
                    ContentTemplate.NEWSLETTER -> {
                        newsletterBuilder.build(
                            topEvents = params["events"] as? List<String> ?: emptyList(),
                            bigMoverSymbol = params["asset"] as? String ?: "BTC",
                            nextWeekOutlook = params["outlook"] as? String ?: "Neutral",
                        )
                    }
                }
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
