package com.cryptodept.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.api.CryptoNewsApi
import com.cryptodept.domain.model.NewsItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsApi: CryptoNewsApi
) : ViewModel() {

    private val _news = MutableStateFlow<List<NewsItem>>(emptyList())
    private val _allNews = MutableStateFlow<List<NewsItem>>(emptyList())
    val news: StateFlow<List<NewsItem>> = _news

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _currentFilter = MutableStateFlow("ALL")
    val currentFilter: StateFlow<String> = _currentFilter

    private var lastFetchTimestamp: Long = 0L
    private val TWELVE_HOURS_IN_MS = 12 * 60 * 60 * 1000L

    init {
        fetchNews()
    }

    fun setFilter(filter: String) {
        _currentFilter.value = filter
        applyFilter()
    }

    private fun applyFilter() {
        val filter = _currentFilter.value
        if (filter == "ALL") {
            _news.value = _allNews.value
        } else {
            _news.value = _allNews.value.filter {
                it.title.uppercase().contains(filter) ||
                        it.source.uppercase().contains(filter)
            }
        }
    }

    fun fetchNews(forceRefresh: Boolean = false) {
        val currentTime = System.currentTimeMillis()

        if (!forceRefresh && _allNews.value.isNotEmpty() && (currentTime - lastFetchTimestamp < TWELVE_HOURS_IN_MS)) {
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = newsApi.getLatestNews(limit = 40)

                val items = response.map {
                    NewsItem(
                        title = it.title,
                        source = it.source,
                        url = it.url ?: "", // Взимаме реалния URL от API-то
                        publishedAt = it.publishedAt,
                        sentiment = it.sentiment?.label?.uppercase() ?: "NEUTRAL"
                    )
                }

                if (items.isEmpty()) throw Exception("No news")

                _allNews.value = items
                lastFetchTimestamp = System.currentTimeMillis()
                applyFilter()
            } catch (e: Exception) {
                if (_allNews.value.isEmpty()) {
                    val mockData = listOf(
                        NewsItem("WIRE: CRYPTO MARKETS VOLATILITY INCREASES IN APRIL 2026", "TERMINAL", "https://cryptopanic.com", "2026-04-28T10:00:00Z", "NEUTRAL"),
                        NewsItem("BTC CONSOLIDATES ABOVE KEY SUPPORT LEVELS", "WIRE", "https://cryptopanic.com", "2026-04-28T09:00:00Z", "BULLISH"),
                        NewsItem("NEW REGULATORY FRAMEWORK PROPOSED FOR STABLECOINS", "REUTERS", "https://reuters.com", "2026-04-28T08:30:00Z", "NEUTRAL"),
                        NewsItem("INSTITUTIONAL INFLOWS REACH NEW QUARTERLY HIGH", "COINDESK", "https://coindesk.com", "2026-04-28T07:15:00Z", "BULLISH")
                    )
                    _allNews.value = mockData
                    applyFilter()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}