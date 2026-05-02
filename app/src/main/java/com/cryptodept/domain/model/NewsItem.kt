package com.cryptodept.domain.model

data class NewsItem(
    val id: String,
    val title: String,
    val url: String,
    val source: String,
    val publishedAt: Long,
    val sentiment: NewsSentiment,
    val currencies: List<String>
)

enum class NewsSentiment { BULLISH, BEARISH, NEUTRAL }
