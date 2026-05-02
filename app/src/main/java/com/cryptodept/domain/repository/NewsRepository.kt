package com.cryptodept.domain.repository

import com.cryptodept.domain.model.NewsItem
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun getNews(currencies: String? = null): Flow<List<NewsItem>>
    suspend fun refreshNews(currencies: String? = null): Result<Unit>
}
