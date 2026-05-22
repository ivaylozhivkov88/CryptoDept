package com.cryptodept.ui.paywall

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.cryptodept.domain.tier.FeatureKey
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.BillingViewModel
import com.cryptodept.util.TestModeFlag
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun PaywallScreen(
    reason: String = "general",
    featureContext: FeatureKey? = null,
    viewModel: BillingViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {},
) {
    val colors = LocalTerminalColors.current
    val subscriptions by viewModel.subscriptions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedProduct by remember { mutableStateOf<ProductDetails?>(null) }

    // Auto-select best value (yearly) if available
    LaunchedEffect(subscriptions) {
        if (subscriptions.isNotEmpty()) {
            selectedProduct = subscriptions.find { it.productId == "pro_1y" }
                ?: subscriptions.find { it.productId == "pro_30d" }
                ?: subscriptions.firstOrNull()
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(colors.background)) {
        
        // === TOP BAR ===
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ">>> ACCESS_CONTROL",
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                
                if (TestModeFlag.SHOW_TEST_PURCHASE_BUTTON) {
                    TextButton(onClick = { 
                        scope.launch {
                            viewModel.billingManager.setAdminOverride(true)
                        }
                    }) {
                        Text("[DEBUG_UNLOCK]", color = colors.amber, fontSize = 10.sp)
                    }
                }

                IconButton(onClick = onDismiss) {
                    Text("[X]", color = colors.dimText, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // === CONTEXTUAL HIGHLIGHT (CHANGE 3) ===
        featureContext?.let { key ->
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, colors.primary, RectangleShape)
                        .background(colors.primary.copy(alpha = 0.08f))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(
                            text = ">>> YOU TRIED TO ACCESS",
                            color = colors.dimText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = key.displayName,
                            color = colors.primary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = key.proDescription,
                            color = colors.textPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }

        // === HEADER (personalized by reason) ===
        item {
            val pitchTitle = when (reason) {
                "whale_tracker" -> "🐋 Unlock Live Whale Activity"
                "predictions" -> "📊 Get All 6 AI Predictions"
                "backtester" -> "🛠️ Unlock Strategy Backtester"
                "derivatives" -> "📈 See Derivatives Data"
                "defi" -> "🏦 Access DeFi Yields"
                "alerts" -> "🔔 Unlimited Alerts"
                "markets" -> "📋 Top 100 Markets"
                "ai_narrative" -> "🤖 Full AI Narrative"
                "watchlist" -> "⭐ Unlimited Watchlist"
                else -> ">>> UPGRADE_TO_PRO"
            }
            
            Text(
                text = pitchTitle,
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(16.dp),
            )
        }
        
        // === VALUE PROPS ===
        item {
            Text(
                text = ">>> WHAT_PRO_UNLOCKS",
                color = colors.amber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
        
        items(proValueProps) { prop ->
            ValuePropRow(
                icon = prop.icon,
                title = prop.title,
                description = prop.description,
            )
        }
        
        // === PRICING TIERS ===
        item {
            Spacer(Modifier.height(24.dp))
            Text(
                text = ">>> CHOOSE_YOUR_PLAN",
                color = colors.amber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
        
        if (isLoading && subscriptions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            }
        } else if (subscriptions.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("MARKET GATEWAY OFFLINE", color = colors.danger, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadSubscriptions() }, shape = RectangleShape) {
                        Text("RETRY")
                    }
                }
            }
        } else {
            items(subscriptions) { product ->
                val isSelected = selectedProduct?.productId == product.productId
                PricingCard(
                    product = product,
                    isSelected = isSelected,
                    onClick = { selectedProduct = product }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        selectedProduct?.let {
                            viewModel.purchase(context as Activity, it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(56.dp),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
                ) {
                    Text("ACTIVATE PRO ACCESS", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                
                TextButton(
                    onClick = { /* Restore logic */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("RESTORE PREVIOUS PURCHASES", color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
        
        // === FREE TIER REASSURANCE ===
        item {
            Spacer(Modifier.height(24.dp))
            Surface(
                color = colors.primary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, colors.primary),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RectangleShape
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = ">>> FREE_TIER_REMAINS_POWERFUL",
                        color = colors.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Even without Pro you get:\n" +
                               "• Live prices (top 10 dashboard)\n" +
                               "• Position Sizer (risk calculator)\n" +
                               "• Daily AI Pick + market pulse\n" +
                               "• News + sentiment + glossary\n" +
                               "• 3 alerts + 1 watchlist (3 coins)\n" +
                               "• Access to top 50 markets\n\n" +
                               "Upgrade to Pro for 15 watchlist slots.",
                        color = colors.textPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        
        // === HONEST DISCLAIMERS ===
        item {
            Surface(
                color = colors.background,
                border = BorderStroke(1.dp, colors.amber),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RectangleShape
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = ">>> WHAT_WE_WONT_DO",
                        color = colors.amber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "We won't promise '100x gains'",
                        "We won't hide our prediction accuracy",
                        "We won't sell your data",
                        "We won't show ads",
                        "We won't trap you in long subscriptions",
                    ).forEach { line ->
                        Text(
                            text = "• $line",
                            color = colors.textPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
        
        // === LEGAL DISCLAIMER ===
        item {
            Text(
                text = "Cryptocurrency trading carries significant risk. " +
                       "All AI predictions are statistical estimates, not financial advice. " +
                       "Past performance does not predict future results. " +
                       "Always do your own research.",
                color = colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ValuePropRow(icon: String, title: String, description: String) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 20.sp, modifier = Modifier.width(32.dp))
        Column {
            Text(title, color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Text(description, color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun PricingCard(product: ProductDetails, isSelected: Boolean, onClick: () -> Unit) {
    val colors = LocalTerminalColors.current
    
    val label = when(product.productId) {
        "pro_1d" -> "1 DAY ACCESS"
        "pro_3d" -> "3 DAYS ACCESS"
        "pro_7d" -> "1 WEEK ACCESS"
        "pro_30d" -> "MONTHLY OPERATOR"
        "pro_90d" -> "QUARTERLY COMMAND"
        "pro_1y" -> "ANNUAL INTELLIGENCE"
        else -> product.name
    }
    
    val price = if (product.productType == "subs") {
        product.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "N/A"
    } else {
        product.oneTimePurchaseOfferDetails?.formattedPrice ?: "N/A"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(1.dp, if (isSelected) colors.primary else colors.grid, RectangleShape)
            .background(if (isSelected) colors.primary.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = if (isSelected) colors.primary else colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Text(price, color = colors.primary, fontWeight = FontWeight.Black, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

private data class ValuePropData(
    val icon: String,
    val title: String,
    val description: String,
)

private val proValueProps = listOf(
    ValuePropData("🐋", "Live Whale Tracker", "Real-time $500k+ transactions on BTC, ETH, SOL"),
    ValuePropData("📊", "All 6 Prediction Engines", "With honest accuracy tracking per engine"),
    ValuePropData("📈", "Real Derivatives Data", "Funding rates, open interest from Binance"),
    ValuePropData("🛠️", "Pro Trader Toolkit", "Backtester, MTF analyzer, entry scorer"),
    ValuePropData("🔔", "Unlimited Alerts", "With composite AND/OR logic"),
    ValuePropData("📚", "DeFi + Macro Analysis", "DefiLlama yields, S&P correlation"),
    ValuePropData("📋", "Top 100 Markets", "50 more coins + advanced filters"),
    ValuePropData("📓", "Full Trade Journal", "Unlimited entries with detailed analytics"),
)
