package com.cryptodept.domain.repository

import androidx.paging.PagingData
import com.cryptodept.domain.model.NewsItem
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun getNews(currencies: String? = null): Flow<List<NewsItem>>

    fun getNewsPagingData(): Flow<PagingData<NewsItem>>

    suspend fun refreshNews(currencies: String? = null): Result<Unit>
}
