package com.cryptodept.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.data.billing.BillingManager
import com.cryptodept.viewmodel.BillingViewModel

@Composable
fun ProGate(
    billingManager: BillingManager = hiltViewModel<BillingViewModel>().billingManager,
    onLocked: @Composable () -> Unit = { /* Paywall handled in NavGraph or here */ },
    content: @Composable () -> Unit
) {
    val isPro by billingManager.isPro.collectAsState()
    
    if (isPro) {
        content()
    } else {
        onLocked()
    }
}
