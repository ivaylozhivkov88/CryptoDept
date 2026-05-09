package com.cryptodept.ui.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.ui.components.TerminalErrorOverlay
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.ComparisonUiState
import com.cryptodept.viewmodel.ComparisonViewModel
import java.util.Locale

@Composable
fun ComparisonScreen(
    coin1Id: String,
    coin2Id: String,
    onBack: () -> Unit,
    viewModel: ComparisonViewModel = hiltViewModel(),
) {
    val colors = LocalTerminalColors.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(coin1Id, coin2Id) {
        viewModel.loadComparison(coin1Id, coin2Id)
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ">>> ASSET_COMPARISON_MATRIX",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onBack) {
                Text("[X]", color = colors.danger, fontFamily = FontFamily.Monospace)
            }
        }

        HorizontalDivider(color = colors.grid, modifier = Modifier.padding(vertical = 12.dp))

        when (val state = uiState) {
            is ComparisonUiState.Loading -> {
                TerminalLoadingSkeleton(Modifier.fillMaxSize())
            }
            is ComparisonUiState.Success -> {
                ComparisonContent(state)
            }
            is ComparisonUiState.Error -> {
                TerminalErrorOverlay(message = state.message, onRetry = { viewModel.loadComparison(coin1Id, coin2Id) })
            }
        }
    }
}

@Composable
fun ComparisonContent(state: ComparisonUiState.Success) {
    val colors = LocalTerminalColors.current

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // CORRELATION BANNER
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.amber)
                    .background(colors.amber.copy(alpha = 0.05f))
                    .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("30D_PEARSON_CORRELATION", color = colors.amber, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = String.format(Locale.US, "%.2f", state.correlation),
                    color =
                        if (state.correlation > 0.7) {
                            colors.primary
                        } else if (state.correlation < -0.5) {
                            colors.danger
                        } else {
                            colors.textPrimary
                        },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = getCorrelationLabel(state.correlation),
                    color = colors.dimText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SPLIT SCREEN HEADERS
        Row(modifier = Modifier.fillMaxWidth()) {
            AssetHeader(state.coin1.price, Modifier.weight(1f))
            Box(modifier = Modifier.width(1.dp).height(60.dp).background(colors.grid))
            AssetHeader(state.coin2.price, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = colors.grid)

        // PARALLEL INDICATORS
        ComparisonRow(
            "CURRENT PRICE",
            "$${String.format(Locale.US, "%.2f", state.coin1.price?.currentPrice ?: 0.0)}",
            "$${String.format(Locale.US, "%.2f", state.coin2.price?.currentPrice ?: 0.0)}",
        )

        ComparisonRow(
            "24H CHANGE",
            "${String.format(Locale.US, "%.2f", state.coin1.price?.priceChangePercentage24h ?: 0.0)}%",
            "${String.format(Locale.US, "%.2f", state.coin2.price?.priceChangePercentage24h ?: 0.0)}%",
            isTrend = true,
            val1 = state.coin1.price?.priceChangePercentage24h ?: 0.0,
            val2 = state.coin2.price?.priceChangePercentage24h ?: 0.0,
        )

        ComparisonRow(
            "RSI (14D)",
            String.format(Locale.US, "%.1f", state.coin1.rsi),
            String.format(Locale.US, "%.1f", state.coin2.rsi),
            isRsi = true,
            val1 = state.coin1.rsi,
            val2 = state.coin2.rsi,
        )

        ComparisonRow(
            "FUNDING RATE",
            "${String.format(Locale.US, "%.4f", state.coin1.funding)}%",
            "${String.format(Locale.US, "%.4f", state.coin2.funding)}%",
        )

        ComparisonRow(
            "MARKET CAP",
            formatLargeNumber(state.coin1.price?.marketCap ?: 0.0),
            formatLargeNumber(state.coin2.price?.marketCap ?: 0.0),
        )
    }
}

@Composable
fun AssetHeader(
    price: CoinPrice?,
    modifier: Modifier,
) {
    val colors = LocalTerminalColors.current
    Column(modifier = modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = price?.symbol?.uppercase() ?: "N/A",
            color = colors.primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = price?.name?.uppercase() ?: "UNKNOWN",
            color = colors.dimText,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
fun ComparisonRow(
    label: String,
    text1: String,
    text2: String,
    isTrend: Boolean = false,
    isRsi: Boolean = false,
    val1: Double = 0.0,
    val2: Double = 0.0,
) {
    val colors = LocalTerminalColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = label,
            color = colors.amber,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val color1 =
                if (isTrend) {
                    (if (val1 >= 0) colors.primary else colors.danger)
                } else if (isRsi) {
                    (
                        if (val1 > 70) {
                            colors.danger
                        } else if (val1 < 30) {
                            colors.primary
                        } else {
                            colors.textPrimary
                        }
                    )
                } else {
                    colors.textPrimary
                }

            val color2 =
                if (isTrend) {
                    (if (val2 >= 0) colors.primary else colors.danger)
                } else if (isRsi) {
                    (
                        if (val2 > 70) {
                            colors.danger
                        } else if (val2 < 30) {
                            colors.primary
                        } else {
                            colors.textPrimary
                        }
                    )
                } else {
                    colors.textPrimary
                }

            Text(
                text1,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = color1,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                "VS",
                modifier = Modifier.padding(horizontal = 8.dp),
                color = colors.grid,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text2,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = color2,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
        HorizontalDivider(color = colors.grid.copy(alpha = 0.3f), modifier = Modifier.padding(top = 12.dp))
    }
}

fun getCorrelationLabel(c: Double) =
    when {
        c > 0.8 -> "EXTREME_POSITIVE_LOCKSTEP"
        c > 0.5 -> "STRONG_POSITIVE_RELATION"
        c > 0.2 -> "MODERATE_POSITIVE"
        c < -0.8 -> "EXTREME_INVERSE_HEDGE"
        c < -0.5 -> "STRONG_INVERSE"
        else -> "DECOUPLED_IDIOSYNCRATIC"
    }

fun formatLargeNumber(n: Double): String =
    when {
        n >= 1_000_000_000_000 -> String.format(Locale.US, "$%.1fT", n / 1_000_000_000_000)
        n >= 1_000_000_000 -> String.format(Locale.US, "$%.1fB", n / 1_000_000_000)
        n >= 1_000_000 -> String.format(Locale.US, "$%.1fM", n / 1_000_000)
        else -> String.format(Locale.US, "$%.0f", n)
    }
