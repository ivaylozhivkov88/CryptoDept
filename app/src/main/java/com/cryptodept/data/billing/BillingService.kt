package com.cryptodept.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.cryptodept.data.datastore.PreferencesService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

typealias BillingManager = BillingService

@Singleton
class BillingService
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val preferencesService: PreferencesService,
        private val analyticsManager: com.cryptodept.util.AnalyticsService,
    ) : PurchasesUpdatedListener {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private var connectionDeferred = CompletableDeferred<Unit>()

        private val _isPro = MutableStateFlow(false)
        val isPro: StateFlow<Boolean> =
            combine(
                _isPro,
                preferencesService.isPro,
            ) { billingPro, prefPro ->
                billingPro || prefPro
            }.stateIn(scope, SharingStarted.Eagerly, false)

        private val billingClient =
            BillingClient
                .newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build()

        init {
            startConnection()
        }

        fun startConnection() {
            if (billingClient.isReady) {
                if (!connectionDeferred.isCompleted) connectionDeferred.complete(Unit)
                return
            }
            
            if (connectionDeferred.isCompleted) {
                connectionDeferred = CompletableDeferred()
            }

            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            analyticsManager.log("Billing setup finished successfully")
                            connectionDeferred.complete(Unit)
                            queryPurchases()
                        } else {
                            analyticsManager.log("Billing setup failed: ${billingResult.debugMessage}")
                            connectionDeferred.complete(Unit) // Complete even if fail to unblock queries
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        if (!connectionDeferred.isCompleted) connectionDeferred.complete(Unit)
                    }
                },
            )
        }

        private fun queryPurchases() {
            if (!billingClient.isReady) return

            val params =
                QueryPurchasesParams
                    .newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()

            billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val hasActiveSubscription =
                        purchases.any { purchase ->
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED && purchase.isAcknowledged
                        }
                    _isPro.value = hasActiveSubscription
                }
            }
        }

        suspend fun querySubscriptions(): List<ProductDetails> {
            if (!billingClient.isReady) {
                startConnection()
                // Wait up to 5 seconds for connection
                withTimeoutOrNull(5000) {
                    connectionDeferred.await()
                }
            }

            if (!billingClient.isReady) return emptyList()

            val inAppProducts = listOf("pro_1d", "pro_3d", "pro_7d")
            val subProducts = listOf("pro_30d", "pro_90d", "pro_1y")

            val inAppList = inAppProducts.map { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }

            val subList = subProducts.map { id ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }

            return try {
                val inAppResult = if (inAppList.isNotEmpty()) {
                    billingClient.queryProductDetails(
                        QueryProductDetailsParams.newBuilder().setProductList(inAppList).build()
                    ).productDetailsList ?: emptyList()
                } else emptyList()

                val subResult = if (subList.isNotEmpty()) {
                    billingClient.queryProductDetails(
                        QueryProductDetailsParams.newBuilder().setProductList(subList).build()
                    ).productDetailsList ?: emptyList()
                } else emptyList()

                inAppResult + subResult
            } catch (e: Exception) {
                analyticsManager.recordException(e, "Error querying subscriptions")
                emptyList()
            }
        }

        fun launchBillingFlow(
            activity: Activity,
            productDetails: ProductDetails,
        ) {
            analyticsManager.logEvent(
                "purchase_attempt",
                android.os.Bundle().apply {
                    putString("product_id", productDetails.productId)
                },
            )
            
            val paramsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
            
            // Subscriptions require an offer token, In-app purchases do not.
            if (productDetails.productType == BillingClient.ProductType.SUBS) {
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                if (offerToken != null) {
                    paramsBuilder.setOfferToken(offerToken)
                }
            }

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(paramsBuilder.build()))
                .build()

            billingClient.launchBillingFlow(activity, billingFlowParams)
        }

        override fun onPurchasesUpdated(
            billingResult: BillingResult,
            purchases: List<Purchase>?,
        ) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }

        private fun handlePurchase(purchase: Purchase) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                // Handle One-Time Passes
                purchase.products.forEach { productId ->
                    when (productId) {
                        "pro_1d" -> preferencesService.setProExpiry(1)
                        "pro_3d" -> preferencesService.setProExpiry(3)
                        "pro_7d" -> preferencesService.setProExpiry(7)
                    }
                }

                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams =
                        AcknowledgePurchaseParams
                            .newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()

                    scope.launch {
                        val result = billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                            // If it's a subscription, queryPurchases will eventually update _isPro.value
                            // For immediate feedback:
                            _isPro.value = true
                        }
                    }
                } else {
                    _isPro.value = true
                }
            }
        }

        fun verifyPurchase(purchaseToken: String): Boolean {
            // Mock server-side verification using the token
            return purchaseToken.isNotBlank()
        }

        suspend fun setAdminOverride(enabled: Boolean) {
            preferencesService.setProStatus(enabled)
        }
    }
