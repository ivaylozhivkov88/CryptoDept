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
        private val cryptoRepository: com.cryptodept.domain.repository.CryptoRepository,
    ) : ViewModel() {
        val pagingNews: Flow<PagingData<NewsItem>> =
            newsRepository
                .getNewsPagingData()
                .cachedIn(viewModelScope)

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading

        private val _error = MutableStateFlow<String?>(null)
        val error: StateFlow<String?> = _error

        private val _currentFilter = MutableStateFlow("ALL")
        val currentFilter: StateFlow<String> = _currentFilter

        val trackedSymbols: StateFlow<Set<String>> = 
            cryptoRepository.getTrackedCoinPrices()
                .map { list -> list.map { it.symbol.uppercase() }.toSet() + "BTC" }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), setOf("BTC"))

        val news: StateFlow<List<NewsItem>> =
            combine(
                newsRepository.getNews(),
                _currentFilter,
                trackedSymbols
            ) { items, filter, favorites ->
                when (filter) {
                    "ALL" -> {
                        val favored = items.filter { item ->
                            item.currencies.any { c -> favorites.contains(c.uppercase()) } ||
                            favorites.any { fav -> item.title.contains(fav, ignoreCase = true) }
                        }
                        val others = items.filter { it !in favored }
                        favored + others
                    }
                    "BULLISH" -> items.filter { it.sentiment == NewsSentiment.BULLISH }
                    "BEARISH" -> items.filter { it.sentiment == NewsSentiment.BEARISH }
                    "FAVORITES" -> items.filter { item ->
                        item.currencies.any { c -> favorites.contains(c.uppercase()) } ||
                        favorites.any { fav -> item.title.contains(fav, ignoreCase = true) }
                    }
                    else -> items.filter {
                        it.title.contains(filter, ignoreCase = true) ||
                            it.currencies.any { c -> c.equals(filter, ignoreCase = true) }
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        init {
            refresh()
        }

        fun setFilter(filter: String) {
            _currentFilter.value = filter
        }

        fun refresh() {
            viewModelScope.launch {
                _isLoading.value = true
                _error.value = null
                try {
                    // Force refresh with a null parameter to use the internal logic with favorites
                    val result = newsRepository.refreshNews(null)
                    if (result.isFailure) {
                        _error.value = result.exceptionOrNull()?.message ?: "FETCH_ERROR"
                    }
                } catch (e: Exception) {
                    _error.value = e.message ?: "SYSTEM_ERROR"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
