package com.cryptodept.domain.model

data class NewsItem(
    val title: String,
    val source: String,
    val url: String,
    val publishedAt: String,
    val sentiment: String? = null
)
