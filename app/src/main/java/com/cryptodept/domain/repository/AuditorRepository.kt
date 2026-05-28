package com.cryptodept.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuditorRepository {
    suspend fun validatePurchase(productId: String, purchaseToken: String): Result<String>
    fun observeUserTier(uid: String): Flow<String?>
}
