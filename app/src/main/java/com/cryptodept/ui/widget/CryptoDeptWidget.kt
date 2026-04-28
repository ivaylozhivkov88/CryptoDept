package com.cryptodept.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.cryptodept.data.db.CoinDao
import com.cryptodept.data.db.CoinEntity
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
        fun coinDao(): CoinDao
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val coinDao = entryPoint.coinDao()
        
        // Fetch top 3 tracked coins
        val coins = try {
            coinDao.getTrackedCoins().first().take(3)
        } catch (e: Exception) {
            emptyList()
        }

        provideContent {
            WidgetContent(coins)
        }
    }

    @Composable
    private fun WidgetContent(coins: List<CoinEntity>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(8.dp)
        ) {
            Text(
                text = ">>> CRYPTODEPT_TERMINAL",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF00FF41)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(Color(0xFF1A2E1A)))
            Spacer(modifier = GlanceModifier.height(4.dp))
            
            if (coins.isEmpty()) {
                Text(
                    text = "NO_DATA_AVAILABLE",
                    style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 10.sp)
                )
            } else {
                coins.forEach { coin ->
                    WidgetRow(
                        symbol = coin.symbol.uppercase(),
                        price = "$${String.format(Locale.US, "%.2f", coin.currentPrice)}",
                        change = "${if (coin.priceChangePercentage24h >= 0) "+" else ""}${String.format(Locale.US, "%.1f", coin.priceChangePercentage24h)}%"
                    )
                }
            }
            
            Spacer(modifier = GlanceModifier.defaultWeight())
            
            Text(
                text = "SYNC: ${java.text.SimpleDateFormat("HH:mm", Locale.US).format(java.util.Date())}",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF007A1F)),
                    fontSize = 9.sp
                )
            )
        }
    }

    @Composable
    private fun WidgetRow(symbol: String, price: String, change: String) {
        val isPositive = !change.startsWith("-")
        val changeColor = if (isPositive) Color(0xFF00FF41) else Color(0xFFFF3B30)
        
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = symbol,
                modifier = GlanceModifier.width(40.dp),
                style = TextStyle(color = ColorProvider(Color(0xFFFFB000)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = price,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp)
            )
            Text(
                text = change,
                style = TextStyle(color = ColorProvider(changeColor), fontSize = 11.sp)
            )
        }
    }
}

class CryptoDeptWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CryptoDeptWidget()
}
