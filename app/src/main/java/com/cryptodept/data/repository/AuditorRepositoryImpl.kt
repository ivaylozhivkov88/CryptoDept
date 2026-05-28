package com.cryptodept.data.repository

import com.cryptodept.data.remote.source.FirebaseRemoteDataSource
import com.cryptodept.domain.repository.AuditorRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuditorRepositoryImpl @Inject constructor(
    private val functions: FirebaseFunctions,
    private val remoteDataSource: FirebaseRemoteDataSource
) : AuditorRepository {

    override suspend fun validatePurchase(productId: String, purchaseToken: String): Result<String> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure(Exception("USER_NOT_LOGGED_IN"))
        
        val data = hashMapOf(
            "uid" to uid,
            "productId" to productId,
            "purchaseToken" to purchaseToken,
            "packageName" to "com.cryptodept"
        )

        return try {
            val result = functions
                .getHttpsCallable("validatePurchase")
                .call(data)
                .await()

            val response = result.data as? Map<String, Any>
            val status = response?.get("status") as? String
            
            if (status == "SUCCESS") {
                Result.success(response["tier"] as? String ?: "PRO")
            } else {
                Result.failure(Exception(response?.get("reason") as? String ?: "VALIDATION_FAILED"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeUserTier(uid: String): Flow<String?> {
        return remoteDataSource.getUserTier(uid)
    }
}
