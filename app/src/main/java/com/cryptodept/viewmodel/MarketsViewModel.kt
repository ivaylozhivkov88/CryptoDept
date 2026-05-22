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
        private val sentimentAnalyzer: com.cryptodept.domain.usecase.SentimentAnalyzer,
        private val errorMapper: com.cryptodept.util.ErrorMessageMapper,
        private val demoMode: com.cryptodept.util.DemoModeProvider,
        private val tierAccessManager: TierAccessManager,
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
                        // ULTRA-OPTIMIZED TRAFFIC (M1.3):
                        // 1. Always include Top 5 by Market Cap (Global)
                        val top5 = coins.sortedByDescending { it.marketCap }.take(5)
                        
                        // 2. Include User's Watchlist (Limit 3 Free / 15 Pro)
                        val limit = if (tier.canAccess(AccessTier.PRO)) 15 else 3
                        val watchlist = coins.filter { it.isTracked }.take(limit)
                        
                        // 3. Combine and Deduplicate
                        val processedCoins = (top5 + watchlist).distinctBy { it.id }

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

        fun search(query: String) {
            viewModelScope.launch(Dispatchers.IO) {
                if (query.isBlank()) {
                    if (top100Cache == null) {
                        top100Cache = cryptoRepository.getAllCoinPrices()
                            .first()
                            .sortedByDescending { it.marketCap }
                            .take(100)
                    }
                    _searchResults.value = top100Cache ?: emptyList()
                } else if (query.length >= 2) {
                    val allCoins = cryptoRepository.getAllCoinPrices().first()
                    _searchResults.value = allCoins.filter {
                        it.symbol.contains(query, true) || it.name.contains(query, true)
                    }
                } else {
                    _searchResults.value = emptyList()
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
                        _errorChannel.trySend("Watchlist limit reached (3 coins). Upgrade to PRO for 15 slots.")
                        return@launch
                    }
                }

                cryptoRepository
                    .toggleTracking(coinId)
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
