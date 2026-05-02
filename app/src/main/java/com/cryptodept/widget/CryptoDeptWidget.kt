package com.cryptodept.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.cryptodept.MainActivity
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.repository.CryptoRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.Locale

class CryptoDeptWidget : GlanceAppWidget() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun cryptoRepository(): CryptoRepository
        fun billingManager(): com.cryptodept.data.billing.BillingManager
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val repository = entryPoint.cryptoRepository()
        val isPro = entryPoint.billingManager().isPro.value
        
        val prices = if (isPro) {
            try {
                repository.getTrackedCoinPrices().first().take(3)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        provideContent {
            CryptoDeptWidgetContent(prices, isPro)
        }
    }

    @Composable
    private fun CryptoDeptWidgetContent(prices: List<CoinPrice>, isPro: Boolean) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(8.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CRYPTODEPT",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFFFFB000)), // Amber
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "LIVE",
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF00FF41)), // Green
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(Color(0xFF003B00))) {}
            
            Spacer(modifier = GlanceModifier.height(8.dp))

            if (!isPro) {
                Column(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ">>> PRO ONLY",
                        style = TextStyle(color = ColorProvider(Color(0xFFFFB000)), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "ACTIVATE IN SETTINGS",
                        style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 10.sp)
                    )
                }
            } else if (prices.isEmpty()) {
                Text(
                    text = "NO_DATA_AVAILABLE",
                    style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 10.sp)
                )
            } else {
                prices.forEach { coin ->
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = coin.symbol.uppercase(),
                            modifier = GlanceModifier.width(40.dp),
                            style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp)
                        )
                        Text(
                            text = "$${String.format(Locale.US, "%,.0f", coin.currentPrice)}",
                            modifier = GlanceModifier.defaultWeight(),
                            style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp, textAlign = TextAlign.End)
                        )
                        val changeColor = if (coin.priceChangePercentage24h >= 0) Color(0xFF00FF41) else Color(0xFFFF3B30)
                        Text(
                            text = "${if (coin.priceChangePercentage24h >= 0) "▲" else "▼"}${String.format(Locale.US, "%.1f", Math.abs(coin.priceChangePercentage24h))}%",
                            modifier = GlanceModifier.width(50.dp),
                            style = TextStyle(color = ColorProvider(changeColor), fontSize = 11.sp, textAlign = TextAlign.End)
                        )
                    }
                }
            }
            
            Spacer(modifier = GlanceModifier.defaultWeight())
            
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(Color(0xFF003B00))) {}
            
            Spacer(modifier = GlanceModifier.height(4.dp))
            
            Text(
                text = "TERMINAL_STATUS: ONLINE",
                style = TextStyle(color = ColorProvider(Color(0xFF008F11)), fontSize = 9.sp)
            )
        }
    }
}
