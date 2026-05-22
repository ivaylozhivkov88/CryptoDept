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
            setOf(SMALL_SQUARE, HORIZONTAL_RECT, LARGE_SQUARE),
        )

    companion object {
        private val SMALL_SQUARE = DpSize(100.dp, 100.dp)
        private val HORIZONTAL_RECT = DpSize(200.dp, 100.dp)
        private val LARGE_SQUARE = DpSize(200.dp, 200.dp)

        val SYMBOL_KEY = ActionParameters.Key<String>("symbol")
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun cryptoRepository(): CryptoRepository
        fun billingService(): BillingService
        fun firebaseDataSource(): com.cryptodept.data.remote.source.FirebaseRemoteDataSource
    }

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val repository = entryPoint.cryptoRepository()
        val isPro = entryPoint.billingService().isPro.value
        val firebaseDataSource = entryPoint.firebaseDataSource()

        val prices =
            try {
                repository.getTrackedCoinPrices().first()
            } catch (e: Exception) {
                emptyList()
            }
            
        val verdict = try {
            val state = firebaseDataSource.getTerminalState().first()
            state?.aiNarrative ?: "MARKET SCANNING..."
        } catch (e: Exception) {
            "OFFLINE"
        }

        provideContent {
            val size = LocalSize.current
            CryptoDeptWidgetContent(prices, isPro, size, verdict)
        }
    }

    @Composable
    private fun CryptoDeptWidgetContent(
        prices: List<CoinPrice>,
        isPro: Boolean,
        size: DpSize,
        verdict: String
    ) {
        val colors = WidgetColors()

        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(8.dp),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start,
        ) {
            WidgetHeader(colors)

            Spacer(modifier = GlanceModifier.height(4.dp))
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(colors.darkGreen)) {}
            Spacer(modifier = GlanceModifier.height(4.dp))

            if (!isPro) {
                ProLockedContent(colors)
            } else if (prices.isEmpty()) {
                Text(text = "NO_DATA", style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 10.sp))
            } else {
                when {
                    size.height >= LARGE_SQUARE.height -> {
                        FullDashboardContent(prices.take(8), colors)
                    }
                    size.width >= HORIZONTAL_RECT.width -> {
                        CompactListContent(prices.take(5), colors)
                    }
                    else -> {
                        SingleCoinContent(prices.firstOrNull(), colors)
                    }
                }
            }

            Spacer(modifier = GlanceModifier.padding(0.dp))
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(colors.darkGreen)) {}
            
            Text(
                text = "VERDICT: ${verdict.take(40)}...",
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 9.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )

            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "T_ID: ${System.currentTimeMillis() % 1000} | OK",
                    style = TextStyle(color = ColorProvider(colors.dimGreen), fontSize = 8.sp),
                )
                Spacer(modifier = GlanceModifier.padding(0.dp))
                Text(
                    text = "[REFRESH]",
                    style = TextStyle(color = ColorProvider(colors.amber), fontSize = 8.sp, fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
                )
            }
        }
    }

    @Composable
    private fun WidgetHeader(colors: WidgetColors) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "CRYPTODEPT",
                style = TextStyle(color = ColorProvider(colors.amber), fontSize = 11.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
            )
            Spacer(modifier = GlanceModifier.padding(0.dp))
            Text(text = "LIVE", style = TextStyle(color = ColorProvider(colors.green), fontSize = 9.sp))
        }
    }

    @Composable
    private fun SingleCoinContent(
        coin: CoinPrice?,
        colors: WidgetColors,
    ) {
        if (coin == null) return
        Column(
            modifier =
                GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionStartActivity<MainActivity>(actionParametersOf(SYMBOL_KEY to coin.symbol))),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = coin.symbol.uppercase(),
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp, fontWeight = FontWeight.Bold),
                )
                Spacer(modifier = GlanceModifier.padding(0.dp))
                val changeColor = if (coin.priceChangePercentage24h >= 0) colors.green else colors.red
                Text(
                    text = "${String.format(Locale.US, "%.1f", coin.priceChangePercentage24h)}%",
                    style = TextStyle(color = ColorProvider(changeColor), fontSize = 12.sp),
                )
            }
            Text(
                text = "$${String.format(Locale.US, "%,.2f", coin.currentPrice)}",
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 20.sp, fontWeight = FontWeight.Bold),
            )
        }
    }

    @Composable
    private fun CompactListContent(
        prices: List<CoinPrice>,
        colors: WidgetColors,
    ) {
        prices.forEach { coin ->
            Row(
                modifier =
                    GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp)
                        .clickable(actionStartActivity<MainActivity>(actionParametersOf(SYMBOL_KEY to coin.symbol))),
            ) {
                Text(text = coin.symbol.uppercase(), style = TextStyle(color = ColorProvider(Color.White), fontSize = 11.sp))
                Spacer(modifier = GlanceModifier.padding(0.dp))
                Text(
                    text = "$${String.format(Locale.US, "%,.0f", coin.currentPrice)}",
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 11.sp),
                )
                val changeColor = if (coin.priceChangePercentage24h >= 0) colors.green else colors.red
                Text(
                    text = " ${if (coin.priceChangePercentage24h >= 0) "+" else ""}${String.format(
                        Locale.US,
                        "%.1f",
                        coin.priceChangePercentage24h,
                    )}%",
                    style = TextStyle(color = ColorProvider(changeColor), fontSize = 10.sp),
                    modifier = GlanceModifier.width(45.dp),
                )
            }
        }
    }

    @Composable
    private fun FullDashboardContent(
        prices: List<CoinPrice>,
        colors: WidgetColors,
    ) {
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            prices.chunked(2).forEach { row ->
                Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    row.forEach { coin ->
                        Column(
                            modifier =
                                GlanceModifier
                                    .padding(4.dp)
                                    .background(Color(0xFF0A0A0A))
                                    .clickable(actionStartActivity<MainActivity>(actionParametersOf(SYMBOL_KEY to coin.symbol))),
                        ) {
                            Text(text = coin.symbol.uppercase(), style = TextStyle(color = ColorProvider(colors.amber), fontSize = 10.sp))
                            Text(
                                text = "$${String.format(Locale.US, "%,.0f", coin.currentPrice)}",
                                style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp),
                            )
                        }
                    }
                    if (row.size == 1) Spacer(modifier = GlanceModifier.width(1.dp))
                }
            }
        }
    }

    @Composable
    private fun ProLockedContent(colors: WidgetColors) {
        Column(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ">>> PRO ONLY",
                style = TextStyle(color = ColorProvider(colors.amber), fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
            Text(text = "ACCESS RESTRICTED", style = TextStyle(color = ColorProvider(Color.Gray), fontSize = 10.sp))
        }
    }
}
