package com.cryptodept.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.data.billing.BillingService
import com.cryptodept.viewmodel.BillingViewModel

@Composable
fun ProGate(
    billingService: BillingService = hiltViewModel<BillingViewModel>().billingService,
    onLocked: @Composable () -> Unit = { /* Paywall handled in NavGraph or here */ },
    content: @Composable () -> Unit,
) {
    val isPro by billingService.isPro.collectAsState()

    if (isPro) {
        content()
    } else {
        onLocked()
    }
}
