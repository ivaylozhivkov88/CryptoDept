package com.cryptodept.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface FearGreedApi {
    @GET("fng/")
    suspend fun getFearGreedIndex(
        @Query("limit") limit: Int = 1
    ): FearGreedResponse
}

data class FearGreedResponse(
    val name: String,
    val data: List<FearGreedData>
)

data class FearGreedData(
    val value: String,
    @com.google.gson.annotations.SerializedName("value_classification")
    val valueClassification: String,
    val timestamp: String
)
