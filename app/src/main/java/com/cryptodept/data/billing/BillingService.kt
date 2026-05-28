package com.cryptodept.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.cryptodept.data.datastore.SubscriptionAccessManager
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
        private val subscription: SubscriptionAccessManager,
        private val analyticsManager: com.cryptodept.util.AnalyticsService,
        private val auditorRepository: com.cryptodept.domain.repository.AuditorRepository,
    ) : PurchasesUpdatedListener {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        private var connectionDeferred = CompletableDeferred<Unit>()

        private val _isPro = MutableStateFlow(false)
        val isPro: StateFlow<Boolean> =
            combine(
                _isPro,
                subscription.isPro,
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
            // Auditor v1.2: Seed initial state from local cache to prevent offline flickering
            _isPro.value = subscription.isPro.value
            startConnection()
            observeServerTier()
        }

        private fun observeServerTier() {
            scope.launch {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    auditorRepository.observeUserTier(uid).collect { tier ->
                        if (tier == "PRO") {
                            android.util.Log.i("AUDITOR", "Server-side PRO status confirmed for user $uid")
                            _isPro.value = true
                            subscription.setProStatus(true)
                        } else if (tier == "FREE") {
                            // Only downgrade if not manually overridden or having local pass
                            // For now, let's keep it simple: if server says FREE, and no local pass, then FREE
                            if (!subscription.isAdmin.value) {
                                // checkProStatus handles local expiry
                                subscription.checkProStatus() 
                            }
                        }
                    }
                }
            }
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
                val now = System.currentTimeMillis()
                val isAdmin = subscription.isAdmin.value

                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val hasActiveSubscription =
                        purchases.any { purchase ->
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED && purchase.isAcknowledged
                        }
                    
                    android.util.Log.d("AUDITOR", "Billing check successful. Active: $hasActiveSubscription")
                    
                    // Admins always get Pro, otherwise use actual billing status
                    val finalProState = isAdmin || hasActiveSubscription
                    _isPro.value = finalProState
                    
                    subscription.setProStatus(finalProState)
                    subscription.setLastBillingCheck(now)
                } else {
                    // Auditor v1.2: STRICT for users, INFINITE for admins
                    if (isAdmin) {
                        android.util.Log.i("AUDITOR", "Offline: Admin identity verified. Maintaining Elite status.")
                        _isPro.value = true
                    } else {
                        android.util.Log.w("AUDITOR", "Billing check failed. Strict mode: Restricted access.")
                        // For regular users, no fresh validation = no Pro, 
                        // unless they have a local One-Time pass (handled via subscription.isPro)
                        _isPro.value = subscription.isPro.value
                    }
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
                // Auditor O-001: Send to server-side validator (Firebase Cloud Functions)
                scope.launch {
                    val productId = purchase.products.firstOrNull() ?: return@launch
                    val result = auditorRepository.validatePurchase(productId, purchase.purchaseToken)
                    
                    result.onSuccess { tier ->
                        android.util.Log.i("AUDITOR", "Server validation SUCCESS. Tier: $tier")
                        if (tier == "PRO") {
                            _isPro.value = true
                            subscription.setProStatus(true)
                        }
                    }.onFailure { e ->
                        android.util.Log.e("AUDITOR", "Server validation FAILED: ${e.message}")
                        // Fallback to local validation for now if needed, but the goal is strict mode
                    }
                }

                // Handle One-Time Passes (Local Logic)
                purchase.products.forEach { productId ->
                    when (productId) {
                        "pro_1d" -> subscription.setProExpiry(1)
                        "pro_3d" -> subscription.setProExpiry(3)
                        "pro_7d" -> subscription.setProExpiry(7)
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
            subscription.setProStatus(enabled)
        }
    }
