package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.data.remote.model.CloudWhaleAlert
import com.cryptodept.data.remote.source.FirebaseRemoteDataSource
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.repository.CryptoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class SearchResult(
    val assets: List<CoinPrice> = emptyList(),
    val agents: List<AgentSearchItem> = emptyList(),
    val whaleAlerts: List<CloudWhaleAlert> = emptyList()
)

data class AgentSearchItem(
    val id: String,
    val name: String,
    val role: String,
    val status: String = "READY"
)

@HiltViewModel
class UnifiedSearchViewModel @Inject constructor(
    private val cryptoRepository: CryptoRepository,
    private val firebaseDataSource: FirebaseRemoteDataSource
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val agentsList = listOf(
        AgentSearchItem("AGENT-SENTINEL", "TECHNICAL_SENTINEL", "Technical Analysis (TA)"),
        AgentSearchItem("AGENT-SCOUT", "GHOST_WHALE", "On-chain Intelligence"),
        AgentSearchItem("AGENT-PULSE", "SENTIMENT_PULSE", "Social & Macro Sentiment"),
        AgentSearchItem("AGENT-QUANT", "THE_ORACLE", "Predictive Analytics"),
        AgentSearchItem("AGENT-MARKET", "MARKETING_STRATEGIST", "Content Engineering"),
        AgentSearchItem("AGENT-AUDITOR", "FISCAL_TREASURY", "Revenue & Billing Integrity"),
        AgentSearchItem("AGENT-SYSTRACE", "SYSTEM_AUDITOR", "System Health & Stability"),
        AgentSearchItem("AGENT-INTEGRITY", "DATA_VERIFICATION", "Data Integrity & Verification"),
        AgentSearchItem("AGENT-CORE", "ORCHESTRATOR", "Reasoning & Strategy"),
        AgentSearchItem("AGENT-NARRATOR", "MARKET_NARRATOR", "Situational Awareness")
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchResult: StateFlow<SearchResult> = _searchQuery
        .debounce(300L)
        .flatMapLatest { query ->
            if (query.length < 2) {
                _isSearching.value = false
                flowOf(SearchResult())
            } else {
                _isSearching.value = true
                combine(
                    cryptoRepository.getAllCoinPrices(),
                    firebaseDataSource.getWhaleAlerts()
                ) { coins, alerts ->
                    val filteredAssets = coins.filter { 
                        it.symbol.contains(query, true) || it.name.contains(query, true) 
                    }.take(10)

                    val filteredAgents = agentsList.filter {
                        it.id.contains(query, true) || it.name.contains(query, true) || it.role.contains(query, true)
                    }

                    val filteredWhales = alerts.filter {
                        it.asset.contains(query, true) || it.transactionType.contains(query, true)
                    }.take(5)

                    SearchResult(
                        assets = filteredAssets,
                        agents = filteredAgents,
                        whaleAlerts = filteredWhales
                    ).also { _isSearching.value = false }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResult())

    fun updateQuery(query: String) {
        _searchQuery.value = query
    }
}
