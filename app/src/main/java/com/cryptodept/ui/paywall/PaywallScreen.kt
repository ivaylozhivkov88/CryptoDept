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
import com.cryptodept.ui.components.AdminPasswordDialog
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.BillingViewModel

@Composable
fun PaywallScreen(
    viewModel: BillingViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {},
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

    // Auto-select best value (yearly) if available
    LaunchedEffect(subscriptions) {
        if (subscriptions.isNotEmpty()) {
            selectedProduct = subscriptions.find { it.productId == "pro-1y" }
                ?: subscriptions.find { it.productId == "pro-30d" }
                ?: subscriptions.firstOrNull()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 1.0f)) // Strictly opaque
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
                .clickable(enabled = false) { /* Block touches from passing through */ },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = ">>> CRYPTODEPT PRO",
                color = colors.amber,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "[X]",
                color = colors.dimText,
                modifier = Modifier.clickable { onDismiss() },
            )
        }

        Text(
            text = "UNLOCK THE FULL TERMINAL",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Feature List
        val features =
            listOf(
                "Unlimited Tracked Coins",
                "AI Trade Coach (Gemini 1.5)",
                "Strategy Backtester Engine",
                "Prediction Ensemble v2.1",
                "Live Home Screen Widget",
                "Priority API Data Streams",
            )

        features.forEach { feature ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✓", color = colors.primary, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                Text(feature, color = colors.textPrimary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                color = colors.primary,
                trackColor = colors.grid
            )
        }

        if (isLoading && subscriptions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                Text("CONNECTING_TO_MARKET_GATEWAY...", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        } else if (subscriptions.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "UNABLE TO FETCH PRODUCTS",
                    color = colors.danger,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Check your internet connection or Google Play status.",
                    color = colors.dimText,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.loadSubscriptions() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RectangleShape
                ) {
                    Text("RETRY CONNECTION", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // Pricing Options
            subscriptions.forEach { product ->
                val isSelected = selectedProduct?.productId == product.productId
                val label = when(product.productId) {
                    "pro-1d" -> "1 DAY ACCESS"
                    "pro-3d" -> "3 DAYS ACCESS"
                    "pro-7d" -> "1 WEEK ACCESS"
                    "pro-30d" -> "MONTHLY OPERATOR"
                    "pro-90d" -> "QUARTERLY COMMAND"
                    "pro-1y" -> "ANNUAL INTELLIGENCE"
                    else -> product.name
                }
                
                val savingText = when(product.productId) {
                    "pro-90d" -> "SAVE 33% VS MONTHLY"
                    "pro-1y" -> "SAVE 42% — BEST VALUE"
                    else -> null
                }

                val price = if (product.productType == "subs") {
                    product.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.pricingPhases
                        ?.pricingPhaseList
                        ?.firstOrNull()
                        ?.formattedPrice ?: "N/A"
                } else {
                    product.oneTimePurchaseOfferDetails?.formattedPrice ?: "N/A"
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(1.dp, if (isSelected) colors.primary else colors.grid, RectangleShape)
                            .background(if (isSelected) colors.primary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { selectedProduct = product }
                            .padding(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = label,
                                color = if (isSelected) colors.primary else colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                            savingText?.let {
                                Text(it, color = colors.amber, fontSize = 9.sp)
                            }
                        }
                        Text(
                            text = price,
                            color = colors.primary,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "7-DAY FREE TRIAL | CANCEL ANYTIME",
                color = colors.dimText,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
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
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.background,
                    ),
                enabled = subscriptions.isNotEmpty(), // Allow Google Play to handle selection errors
            ) {
                Text("UNLOCK PRO ACCESS →", fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = { /* Restore purchases logic */ },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("RESTORE PURCHASES", color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Terms of Service & Privacy Policy apply.",
            color = colors.grid,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
