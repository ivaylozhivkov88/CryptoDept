package com.cryptodept.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.model.OHLCData
import com.cryptodept.domain.repository.CryptoRepository
import com.cryptodept.domain.repository.DerivativesRepository
import com.cryptodept.domain.usecase.TechnicalAnalysisEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ComparisonData(
    val price: CoinPrice?,
    val rsi: Double,
    val funding: Double,
    val ohlc: List<OHLCData>,
)

sealed class ComparisonUiState {
    object Loading : ComparisonUiState()

    data class Success(
        val coin1: ComparisonData,
        val coin2: ComparisonData,
        val correlation: Double,
    ) : ComparisonUiState()

    data class Error(
        val message: String,
    ) : ComparisonUiState()
}

@HiltViewModel
class ComparisonViewModel
    @Inject
    constructor(
        private val cryptoRepository: CryptoRepository,
        private val derivativesRepository: DerivativesRepository,
        private val taEngine: TechnicalAnalysisEngine,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ComparisonUiState>(ComparisonUiState.Loading)
        val uiState: StateFlow<ComparisonUiState> = _uiState.asStateFlow()

        fun loadComparison(
            id1: String,
            id2: String,
        ) {
            viewModelScope.launch {
                _uiState.value = ComparisonUiState.Loading
                try {
                    val data1 = async(Dispatchers.IO) { fetchAssetData(id1) }
                    val data2 = async(Dispatchers.IO) { fetchAssetData(id2) }

                    val res1 = data1.await()
                    val res2 = data2.await()

                    val correlation = calculateCorrelation(res1.ohlc, res2.ohlc)

                    _uiState.value = ComparisonUiState.Success(res1, res2, correlation)
                } catch (e: Exception) {
                    _uiState.value = ComparisonUiState.Error(e.message ?: "COMPARISON FAILED")
                }
            }
        }

        private suspend fun fetchAssetData(id: String): ComparisonData {
            val price = cryptoRepository.getCoinPrice(id).first()
            val ohlc = cryptoRepository.getOHLCData(id, 30)
            val rsi = taEngine.calculateRSI(ohlc.map { it.close })
            val symbol = price?.symbol?.uppercase() ?: ""
            val funding =
                if (symbol.isNotEmpty()) {
                    derivativesRepository.getFundingRate(symbol).getOrNull()?.binanceRate ?: 0.0
                } else {
                    0.0
                }

            return ComparisonData(price, rsi, funding, ohlc)
        }

        private fun calculateCorrelation(
            ohlc1: List<OHLCData>,
            ohlc2: List<OHLCData>,
        ): Double {
            val prices1 = ohlc1.takeLast(30).map { it.close }
            val prices2 = ohlc2.takeLast(30).map { it.close }

            val size = minOf(prices1.size, prices2.size)
            if (size < 5) return 0.0

            val p1 = prices1.takeLast(size)
            val p2 = prices2.takeLast(size)

            val mean1 = p1.average()
            val mean2 = p2.average()

            var num = 0.0
            var den1 = 0.0
            var den2 = 0.0

            for (i in 0 until size) {
                val d1 = p1[i] - mean1
                val d2 = p2[i] - mean2
                num += d1 * d2
                den1 += d1 * d1
                den2 += d2 * d2
            }

            val den = kotlin.math.sqrt(den1 * den2)
            return if (den != 0.0) num / den else 0.0
        }
    }
