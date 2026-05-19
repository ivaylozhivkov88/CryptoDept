package com.cryptodept.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class NewsItem(
    val id: String,
    val title: String,
    val url: String,
    val source: String,
    val publishedAt: Long,
    val sentiment: NewsSentiment,
    val currencies: ImmutableList<String>,
    val imageUrl: String? = null,
)

enum class NewsSentiment { BULLISH, BEARISH, NEUTRAL }
