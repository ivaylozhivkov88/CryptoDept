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
                        val limit = if (tier.canAccess(AccessTier.PRO)) 200 else 50
                        val sortedCoins = coins.sortedByDescending { it.marketCap }.take(limit)
                        
                        // Task 1.2: Enforce strict 3-star limit for FREE tier in UI state
                        val processedCoins = if (tier == AccessTier.FREE) {
                            var trackedCount = 0
                            sortedCoins.map { coin ->
                                if (coin.isTracked) {
                                    if (trackedCount < 3) {
                                        trackedCount++
                                        coin
                                    } else {
                                        coin.copy(isTracked = false)
                                    }
                                } else coin
                            }
                        } else sortedCoins

                        MarketsUiState.Success(
                            coins = processedCoins,
                            isProUpgradeNeeded = !tier.canAccess(AccessTier.PRO)
                        )
                    } else MarketsUiState.Loading
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MarketsUiState.Loading)

        private val _searchResults = MutableStateFlow<List<CoinPrice>>(emptyList())
        val searchResults: StateFlow<List<CoinPrice>> = _searchResults.asStateFlow()

        private val _sentimentMap = MutableStateFlow<Map<String, com.cryptodept.domain.usecase.SentimentVerdict>>(emptyMap())
        val sentimentMap: StateFlow<Map<String, com.cryptodept.domain.usecase.SentimentVerdict>> = combine(_sentimentMap, demoMode.demoActiveState) { map, active ->
            if (active) {
                demoMode.getDemoMarketsList().associate { it.symbol.lowercase() to com.cryptodept.domain.usecase.SentimentVerdict.BULLISH }
            } else map
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

        private val _errorChannel = Channel<String>()
        val errorEvents = _errorChannel.receiveAsFlow()

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
            if (query.length < 2) {
                _searchResults.value = emptyList()
                return
            }
            viewModelScope.launch(Dispatchers.IO) {
                cryptoRepository.getAllCoinPrices()
                    .first()
                    .filter { it.symbol.contains(query, true) || it.name.contains(query, true) }
                    .let { _searchResults.value = it }
            }
        }

        fun toggleTracking(coinId: String) {
            viewModelScope.launch(Dispatchers.IO) {
                val tier = tierAccessManager.getCurrentTier()
                if (!tier.canAccess(AccessTier.PRO)) {
                    val currentTracked = cryptoRepository.getTrackedCoinPrices().first()
                    val isCurrentlyTracked = currentTracked.any { it.id == coinId }
                    
                    if (!isCurrentlyTracked && currentTracked.size >= 10) {
                        _errorChannel.trySend("Watchlist limit reached (10 coins). Upgrade to PRO for unlimited.")
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
