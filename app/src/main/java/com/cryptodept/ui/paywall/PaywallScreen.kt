package com.cryptodept.ui.paywall

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.android.billingclient.api.ProductDetails
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.BillingViewModel

@Composable
fun PaywallScreen(
    viewModel: BillingViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {}
) {
    val colors = LocalTerminalColors.current
    val subscriptions by viewModel.subscriptions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val analytics = com.cryptodept.ui.components.LocalAnalyticsManager.current
    var selectedProduct by remember { mutableStateOf<ProductDetails?>(null) }

    LaunchedEffect(Unit) {
        analytics?.logProPaywallSeen()
    }

    // Auto-select yearly if available
    LaunchedEffect(subscriptions) {
        if (selectedProduct == null) {
            selectedProduct = subscriptions.find { it.productId == "pro_yearly" } 
                ?: subscriptions.firstOrNull()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = ">>> CRYPTODEPT PRO",
                color = colors.amber,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "[X]",
                color = colors.dimText,
                modifier = Modifier.clickable { onDismiss() }
            )
        }

        Text(
            text = "UNLOCK THE FULL TERMINAL",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Feature List
        val features = listOf(
            "Unlimited Tracked Coins",
            "AI Trade Coach (Gemini 1.5)",
            "Strategy Backtester Engine",
            "Prediction Ensemble v2.1",
            "Live Home Screen Widget",
            "Priority API Data Streams"
        )

        features.forEach { feature ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✓", color = colors.primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                Text(feature, color = colors.textPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        if (isLoading) {
            CircularProgressIndicator(color = colors.primary)
        } else {
            // Pricing Options
            subscriptions.forEach { product ->
                val isYearly = product.productId == "pro_yearly"
                val isSelected = selectedProduct?.productId == product.productId
                val price = product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "N/A"
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.dp, if (isSelected) colors.primary else colors.grid, RectangleShape)
                        .background(if (isSelected) colors.primary.copy(alpha = 0.1f) else Color.Transparent)
                        .clickable { selectedProduct = product }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isYearly) "ANNUAL PLAN" else "MONTHLY PLAN",
                                color = if (isSelected) colors.primary else colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (isYearly) {
                                Text("SAVE 33% COMPARED TO MONTHLY", color = colors.amber, fontSize = 10.sp)
                            }
                        }
                        Text(
                            text = price,
                            color = colors.primary,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "7-DAY FREE TRIAL | CANCEL ANYTIME",
                color = colors.dimText,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    selectedProduct?.let { 
                        analytics?.logProPurchased(it.productId)
                        viewModel.purchase(context as Activity, it)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.background
                ),
                enabled = selectedProduct != null
            ) {
                Text("UNLOCK PRO ACCESS →", fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = { /* Restore purchases logic */ },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("RESTORE PURCHASES", color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }

            var showAdminDialog by remember { mutableStateOf(false) }
            TextButton(
                onClick = { showAdminDialog = true },
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("ADMIN", color = colors.grid, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }

            if (showAdminDialog) {
                var adminCode by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showAdminDialog = false },
                    containerColor = Color.Black,
                    modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
                    title = { Text("ADMIN ACCESS", color = colors.primary, fontFamily = FontFamily.Monospace) },
                    text = {
                        Column {
                            Text("ENTER AUTHORIZATION CODE:", color = colors.dimText, fontSize = 10.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            com.cryptodept.ui.components.TerminalInput(
                                label = "CODE",
                                value = adminCode,
                                onValueChange = { adminCode = it }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            if (viewModel.unlockAdmin(adminCode)) {
                                showAdminDialog = false
                                onDismiss()
                            }
                        }) {
                            Text("EXECUTE", color = colors.primary, fontFamily = FontFamily.Monospace)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAdminDialog = false }) {
                            Text("CANCEL", color = colors.dimText, fontFamily = FontFamily.Monospace)
                        }
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Terms of Service & Privacy Policy apply.",
            color = colors.grid,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
