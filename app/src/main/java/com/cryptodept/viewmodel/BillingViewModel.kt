package com.cryptodept.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.cryptodept.data.billing.BillingService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BillingViewModel
    @Inject
    constructor(
        val billingService: BillingService,
    ) : ViewModel() {
        val billingManager get() = billingService

        private val _subscriptions = MutableStateFlow<List<ProductDetails>>(emptyList())
        val subscriptions: StateFlow<List<ProductDetails>> = _subscriptions.asStateFlow()

        private val _isLoading = MutableStateFlow(false)
        val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        init {
            loadSubscriptions()
        }

        fun loadSubscriptions() {
            viewModelScope.launch {
                _isLoading.value = true
                kotlinx.coroutines.delay(800)
                _subscriptions.value = billingService.querySubscriptions()
                _isLoading.value = false
            }
        }

        fun purchase(
            activity: Activity,
            productDetails: ProductDetails,
        ) {
            billingService.launchBillingFlow(activity, productDetails)
        }

    }
