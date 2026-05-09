package com.cryptodept.domain.repository

import com.cryptodept.domain.model.TechnicalIndicators
import kotlinx.coroutines.flow.Flow

interface AnalysisRepository {
    fun getTechnicalIndicators(coinId: String): Flow<TechnicalIndicators>
}
