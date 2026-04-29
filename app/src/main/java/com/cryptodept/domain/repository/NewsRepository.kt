package com.cryptodept.domain.repository

import com.cryptodept.domain.model.NewsItem
import com.cryptodept.domain.model.RssNewsItem
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    // CryptoPanic (вече имаме)
    fun getCryptoPanicNews(): Flow<List<NewsItem>>

    // RSS feeds (нови)
    suspend fun getRssNews(forceRefresh: Boolean = false): Result<List<RssNewsItem>>
    fun observeRssNews(): Flow<List<RssNewsItem>>

    // Combined (агрегирани от всички източници)
    fun getAllNews(): Flow<List<RssNewsItem>>
}
