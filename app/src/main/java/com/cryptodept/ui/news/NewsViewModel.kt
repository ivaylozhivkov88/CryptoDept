package com.cryptodept.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cryptodept.domain.model.NewsItem
import com.cryptodept.domain.model.NewsSentiment
import com.cryptodept.domain.repository.NewsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel
    @Inject
    constructor(
        private val newsRepository: NewsRepository,
    ) : ViewModel() {
        val pagingNews: Flow<PagingData<NewsItem>> =
            newsRepository
                .getNewsPagingData()
                .cachedIn(viewModelScope)

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading

        private val _currentFilter = MutableStateFlow("ALL")
        val currentFilter: StateFlow<String> = _currentFilter

        val news: StateFlow<List<NewsItem>> =
            combine(
                newsRepository.getNews(),
                _currentFilter,
            ) { items, filter ->
                if (filter == "ALL") {
                    items
                } else if (filter == "BULLISH") {
                    items.filter { it.sentiment == NewsSentiment.BULLISH }
                } else if (filter == "BEARISH") {
                    items.filter { it.sentiment == NewsSentiment.BEARISH }
                } else {
                    items.filter {
                        it.title.contains(filter, ignoreCase = true) ||
                            it.currencies.any { c -> c.equals(filter, ignoreCase = true) }
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        init {
            refresh()
            // Auto-refresh every 5 minutes
            viewModelScope.launch {
                while (true) {
                    delay(5 * 60 * 1000L)
                    refresh()
                }
            }
        }

        fun setFilter(filter: String) {
            _currentFilter.value = filter
        }

        fun refresh() {
            viewModelScope.launch {
                _isLoading.value = true
                try {
                    newsRepository.refreshNews()
                } catch (e: Exception) {
                    // Log error
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
