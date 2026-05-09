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
            billingClient.startConnection(
                object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                            analyticsManager.log("Billing setup finished successfully")
                            queryPurchases()
                        } else {
                            analyticsManager.log("Billing setup failed: ${billingResult.debugMessage}")
                        }
                    }

                    override fun onBillingServiceDisconnected() {
                        // Try to restart connection on next use or with exponential backoff
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
            val productList =
                listOf(
                    QueryProductDetailsParams.Product
                        .newBuilder()
                        .setProductId("pro_monthly")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product
                        .newBuilder()
                        .setProductId("pro_yearly")
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                )

            val params =
                QueryProductDetailsParams
                    .newBuilder()
                    .setProductList(productList)
                    .build()

            return try {
                val result = billingClient.queryProductDetails(params)
                result.productDetailsList ?: emptyList()
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
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return

            val productDetailsParamsList =
                listOf(
                    BillingFlowParams.ProductDetailsParams
                        .newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build(),
                )

            val billingFlowParams =
                BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
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
                if (!purchase.isAcknowledged) {
                    val acknowledgePurchaseParams =
                        AcknowledgePurchaseParams
                            .newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()

                    scope.launch {
                        val result = billingClient.acknowledgePurchase(acknowledgePurchaseParams)
                        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
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
