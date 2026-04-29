package com.cryptodept.domain.model

data class RssNewsItem(
    val id: String,           // hash на title+source
    val title: String,
    val url: String,
    val publishedAt: Long,
    val source: String,       // "CoinTelegraph", "The Block"
    val category: String,     // "General", "Institutional", "Bitcoin", "DeFi"
    val sentiment: NewsSentiment,
    val relevantCoins: List<String>,  // ["BTC", "ETH"] извлечени от заглавието
    val isBreaking: Boolean   // Публикувана < 30 мин назад
)

enum class NewsSentiment { VERY_BULLISH, BULLISH, NEUTRAL, BEARISH, VERY_BEARISH }
