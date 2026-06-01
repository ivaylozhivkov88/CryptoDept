package com.cryptodept.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.background
import com.cryptodept.MainActivity
import com.cryptodept.data.billing.BillingService
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.domain.repository.CryptoRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.Locale

data class WidgetColors(
    val amber: Color = Color(0xFFFFB000),
    val green: Color = Color(0xFF00FF41),
    val red: Color = Color(0xFFFF3B30),
    val darkGreen: Color = Color(0xFF003B00),
    val dimGreen: Color = Color(0xFF008F11),
)

class CryptoDeptWidget : GlanceAppWidget() {
    override val sizeMode =
        SizeMode.Responsive(
            setOf(SMALL_SQUARE, HORIZONTAL_WIDE),
        )

    companion object {
        private val SMALL_SQUARE = DpSize(60.dp, 60.dp) // 1x1
        private val HORIZONTAL_WIDE = DpSize(240.dp, 60.dp) // 4x1

        val SYMBOL_KEY = ActionParameters.Key<String>("symbol")
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun cryptoRepository(): CryptoRepository
        fun billingService(): BillingService
        fun subscriptionManager(): com.cryptodept.data.datastore.SubscriptionAccessManager
        fun firebaseDataSource(): com.cryptodept.data.remote.source.FirebaseRemoteDataSource
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val repository = entryPoint.cryptoRepository()
        val firebaseDataSource = entryPoint.firebaseDataSource()

        val prices =
            try {
                repository.getTrackedCoinPrices().first()
            } catch (e: Exception) {
                emptyList()
            }
            
        provideContent {
            val size = LocalSize.current
            CryptoDeptWidgetContent(prices, size)
        }
    }

    @Composable
    private fun CryptoDeptWidgetContent(
        prices: List<CoinPrice>,
        size: DpSize
    ) {
        val colors = WidgetColors()
        val now = System.currentTimeMillis()

        Box(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (prices.isEmpty()) {
                Text(text = "NO_TRACKERS", style = TextStyle(color = ColorProvider(colors.dimGreen), fontSize = 10.sp))
            } else {
                if (size.width >= HORIZONTAL_WIDE.width) {
                    // 4x1 VARIANT
                    Row(modifier = GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        // Show 4 coins, rotate every minute if more than 4
                        val setIndex = ((now / 60000) % ((prices.size + 3) / 4)).toInt()
                        val visible = prices.drop(setIndex * 4).take(4)
                        
                        visible.forEach { coin ->
                            Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight(), contentAlignment = Alignment.Center) {
                                MiniCoinCell(coin, colors)
                            }
                        }
                    }
                } else {
                    // 1x1 VARIANT
                    // Rotate every 30 seconds
                    val index = ((now / 30000) % prices.size).toInt()
                    val coin = prices[index]
                    SingleCoinCompact(coin, colors)
                }
            }
        }
    }

    @Composable
    private fun MiniCoinCell(coin: CoinPrice, colors: WidgetColors) {
        val formattedPrice = when {
            coin.currentPrice >= 10000 -> String.format(Locale.US, "$%.0f", coin.currentPrice)
            coin.currentPrice >= 1000 -> String.format(Locale.US, "$%.1f", coin.currentPrice)
            else -> String.format(Locale.US, "$%.2f", coin.currentPrice)
        }

        Column(
            modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>(actionParametersOf(SYMBOL_KEY to coin.symbol))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = coin.symbol.uppercase(), style = TextStyle(color = ColorProvider(colors.amber), fontSize = 10.sp, fontWeight = FontWeight.Bold))
            Text(
                text = formattedPrice, 
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
            val changeColor = if (coin.priceChangePercentage24h >= 0) colors.green else colors.red
            Text(
                text = "${if (coin.priceChangePercentage24h >= 0) "+" else ""}${String.format(Locale.US, "%.1f%%", coin.priceChangePercentage24h)}",
                style = TextStyle(color = ColorProvider(changeColor), fontSize = 9.sp)
            )
        }
    }

    @Composable
    private fun SingleCoinCompact(coin: CoinPrice, colors: WidgetColors) {
        val formattedPrice = when {
            coin.currentPrice >= 10000 -> String.format(Locale.US, "$%.0f", coin.currentPrice)
            coin.currentPrice >= 1000 -> String.format(Locale.US, "$%.1f", coin.currentPrice)
            else -> String.format(Locale.US, "$%.2f", coin.currentPrice)
        }

        Column(
            modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<MainActivity>(actionParametersOf(SYMBOL_KEY to coin.symbol))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = coin.symbol.uppercase(),
                style = TextStyle(color = ColorProvider(colors.amber), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = formattedPrice,
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            val changeColor = if (coin.priceChangePercentage24h >= 0) colors.green else colors.red
            Text(
                text = "${if (coin.priceChangePercentage24h >= 0) "+" else ""}${String.format(Locale.US, "%.1f%%", coin.priceChangePercentage24h)}",
                style = TextStyle(color = ColorProvider(changeColor), fontSize = 10.sp)
            )
        }
    }
}
