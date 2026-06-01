package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.tier.AccessTier
import com.cryptodept.domain.tier.TierAccessManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MarketsUiState {
    object Loading : MarketsUiState()

    data class Success(
        val coins: List<CoinPrice>,
        val isProUpgradeNeeded: Boolean = false
    ) : MarketsUiState()

    data class Error(
        val message: String,
    ) : MarketsUiState()
}

@HiltViewModel
class MarketsViewModel
    @Inject
    constructor(
        private val cryptoRepository: CryptoRepository,
        private val errorMapper: com.cryptodept.util.ErrorMessageMapper,
        private val demoMode: com.cryptodept.util.DemoModeProvider,
        val tierAccessManager: TierAccessManager,
    ) : ViewModel() {
        
        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<MarketsUiState> = combine(
            demoMode.demoActiveState,
            tierAccessManager.currentTier
        ) { active: Boolean, tier: AccessTier ->
            active to tier
        }.flatMapLatest { (active, tier) ->
            if (active) {
                flowOf(MarketsUiState.Success(
                    coins = demoMode.getDemoMarketsList().map { it.toDomain() },
                    isProUpgradeNeeded = false
                ))
            } else {
                cryptoRepository.getAllCoinPrices().map { coins ->
                    if (coins.isNotEmpty()) {
                        // 1. Get User's Watchlist (Limit 3 Free / 30 Pro)
                        val watchlistLimit = if (tier.canAccess(AccessTier.PRO)) 30 else 3
                        val watchlist = coins.filter { it.isTracked }.take(watchlistLimit)
                        
                        // 2. Logic: If no favorites, show TOP 5. If favorites exist, show ONLY them.
                        val processedCoins = if (watchlist.isEmpty()) {
                            coins.sortedByDescending { it.marketCap }.take(5)
                        } else {
                            watchlist
                        }

                        MarketsUiState.Success(
                            coins = processedCoins,
                            isProUpgradeNeeded = !tier.canAccess(AccessTier.PRO)
                        ).also { triggerScanLine() }
                    } else MarketsUiState.Loading
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MarketsUiState.Loading)

        private val _searchResults = MutableStateFlow<List<CoinPrice>>(emptyList())
        val searchResults: StateFlow<List<CoinPrice>> = _searchResults.asStateFlow()

        private var top100Cache: List<CoinPrice>? = null

        private val _sentimentMap = MutableStateFlow<Map<String, com.cryptodept.domain.usecase.SentimentVerdict>>(emptyMap())
        val sentimentMap: StateFlow<Map<String, com.cryptodept.domain.usecase.SentimentVerdict>> = combine(_sentimentMap, demoMode.demoActiveState) { map, active ->
            if (active) {
                demoMode.getDemoMarketsList().associate { it.symbol.lowercase() to com.cryptodept.domain.usecase.SentimentVerdict.BULLISH }
            } else map
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        private val _errorChannel = Channel<String>()
        val errorEvents = _errorChannel.receiveAsFlow()

        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

        private var isScanLineTriggered = false
        private fun triggerScanLine() {
            if (isScanLineTriggered) return
            isScanLineTriggered = true
            viewModelScope.launch {
                _isRefreshing.value = true
                delay(150L)
                _isRefreshing.value = false
                delay(1000L) // Debounce
                isScanLineTriggered = false
            }
        }

        init {
            refreshData()
        }

        private fun com.cryptodept.util.DemoMarketCoin.toDomain() = CoinPrice(
            id = symbol.lowercase(),
            symbol = symbol,
            name = name,
            currentPrice = price,
            priceChange24h = (change24h / 100) * price,
            priceChangePercentage24h = change24h,
            marketCap = marketCap.toDouble(),
            totalVolume = 100_000_000.0,
            high24h = price * 1.05,
            low24h = price * 0.95,
            lastUpdated = System.currentTimeMillis(),
            isTracked = true
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        val trackedCoins: StateFlow<List<CoinPrice>> = cryptoRepository.getTrackedCoinPrices()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private var lastSearchQuery: String = ""

        fun search(query: String) {
            lastSearchQuery = query
            viewModelScope.launch(Dispatchers.IO) {
                if (query.isBlank()) {
                    if (top100Cache == null) {
                        top100Cache = cryptoRepository.getAllCoinPrices()
                            .first()
                            .sortedByDescending { it.marketCap }
                            .take(100)
                    }
                    _searchResults.value = top100Cache ?: emptyList()
                } else {
                    val results = cryptoRepository.searchCoins(query)
                    _searchResults.value = results
                }
            }
        }

        fun toggleTracking(coinId: String) {
            viewModelScope.launch(Dispatchers.IO) {
                val tier = tierAccessManager.getCachedTier()
                if (!tier.canAccess(AccessTier.PRO)) {
                    val currentTracked = cryptoRepository.getTrackedCoinPrices().first()
                    val isCurrentlyTracked = currentTracked.any { it.id == coinId }
                    
                    if (!isCurrentlyTracked && currentTracked.size >= 3) {
                        _errorChannel.trySend("Watchlist limit reached (3 coins). Upgrade to PRO for 30 slots.")
                        return@launch
                    }
                }

                cryptoRepository
                    .toggleTracking(coinId)
                    .onSuccess {
                        // Refresh search results to update labels
                        if (lastSearchQuery.isNotEmpty()) {
                            search(lastSearchQuery)
                        }
                    }
                    .onFailure { error ->
                        _errorChannel.trySend(errorMapper.map(error))
                    }
            }
        }

        fun refreshData() {
            viewModelScope.launch(Dispatchers.IO) {
                cryptoRepository.refreshPrices()
            }
        }

        fun loadMarkets() {
            refreshData()
        }
    }
