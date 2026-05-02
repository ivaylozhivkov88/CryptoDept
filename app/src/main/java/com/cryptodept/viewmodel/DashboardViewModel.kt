package com.cryptodept.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.BuildConfig
import com.cryptodept.data.api.BlockchainApi
import com.cryptodept.data.api.EtherscanApi
import com.cryptodept.data.api.FearGreedApi
import com.cryptodept.data.api.GasOracleResult
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.model.NetworkHealth
import com.cryptodept.domain.usecase.GetPricesUseCase
import com.cryptodept.domain.usecase.RefreshPricesUseCase
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getPricesUseCase: GetPricesUseCase,
    private val refreshPricesUseCase: RefreshPricesUseCase,
    private val sentimentAnalyzer: com.cryptodept.domain.usecase.SentimentAnalyzer,
    private val blockchainApi: BlockchainApi,
    private val etherscanApi: EtherscanApi,
    private val fearGreedApi: FearGreedApi,
    private val gson: Gson,
    private val analytics: com.cryptodept.util.AnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _networkHealth = MutableStateFlow<NetworkHealth?>(null)
    val networkHealth: StateFlow<NetworkHealth?> = _networkHealth.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        analytics.logScreenView("DASHBOARD")
        loadPrices()
        fetchNetworkHealth()
    }

    private fun fetchNetworkHealth() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val btcStats = async { blockchainApi.getStats() }
                val ethGas = async {
                    try {
                        etherscanApi.getGasOracle(apiKey = BuildConfig.ETHERSCAN_API_KEY)
                    } catch (e: Exception) {
                        Log.w("CryptoDept_API", "Etherscan failed: ${e.message}")
                        null
                    }
                }
                val fearGreed = async {
                    try {
                        fearGreedApi.getFearGreedIndex()
                    } catch (e: Exception) {
                        Log.w("CryptoDept_API", "Fear&Greed failed: ${e.message}")
                        null
                    }
                }

                val btc = btcStats.await()
                val eth = ethGas.await()
                val fg = fearGreed.await()

                // --- ETH Gas parsing (robust) ---
                var safeGasPrice = "N/A"
                if (eth != null) {
                    try {
                        when {
                            eth.status == "1" && eth.result != null && eth.result.isJsonObject -> {
                                val gasData = gson.fromJson(eth.result, GasOracleResult::class.java)
                                safeGasPrice = "${gasData.SafeGasPrice} Gwei"
                            }
                            eth.result != null && eth.result.isJsonPrimitive -> {
                                Log.w("CryptoDept_API", "Etherscan error response: ${eth.result.asString}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CryptoDept_API", "Gas parse error: ${e.message}")
                    }
                }

                // --- Fear & Greed ---
                val fgValue = fg?.data?.firstOrNull()
                val fearGreedIndex = fgValue?.value?.toIntOrNull() ?: 50
                val fearGreedLabel = fgValue?.valueClassification ?: "NEUTRAL"

                // --- BTC Stats ---
                val hashrateStr = try {
                    "${(btc.hash_rate / 1_000_000_000_000_000_000.0).toInt()} EH/s"
                } catch (e: Exception) { "N/A" }

                val mempoolStr = try {
                    "${btc.mempool_count} TXs"
                } catch (e: Exception) { "N/A" }

                // --- Social Pulse ---
                val pulse = sentimentAnalyzer.calculatePulse("BTC")
                val pulseLabel = sentimentAnalyzer.getPulseLabel(pulse)

                _networkHealth.value = NetworkHealth(
                    btcHashrate = hashrateStr,
                    btcMempool = mempoolStr,
                    ethGas = safeGasPrice,
                    fearGreedIndex = fearGreedIndex,
                    fearGreedLabel = fearGreedLabel,
                    socialPulse = pulse,
                    socialPulseLabel = pulseLabel
                )
            } catch (e: Exception) {
                Log.e("CryptoDept_API", "fetchNetworkHealth error: ${e.message}")
            }
        }
    }

    fun loadPrices() {
        viewModelScope.launch(Dispatchers.IO) {
            getPricesUseCase()
                .onStart { _uiState.value = DashboardUiState.Loading }
                .catch { e -> _uiState.value = DashboardUiState.Error(e.message ?: "DATABASE ERROR") }
                .conflate()
                .collect { prices ->
                    _uiState.value = DashboardUiState.Success(prices)
                }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            refreshPricesUseCase()
            fetchNetworkHealth()
            _isRefreshing.value = false
        }
    }
}

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    data class Success(val prices: List<CoinPrice>) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}