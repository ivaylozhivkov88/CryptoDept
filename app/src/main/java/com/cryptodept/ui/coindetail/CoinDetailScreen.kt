package com.cryptodept.ui.coindetail

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.CoinDetail
import com.cryptodept.domain.model.OHLCData
import com.cryptodept.ui.components.SimpleLineChart
import com.cryptodept.ui.components.SimpleSparkline
import com.cryptodept.ui.components.TerminalErrorOverlay
import com.cryptodept.ui.components.TerminalLoadingSkeleton
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.CoinDetailUiState
import com.cryptodept.viewmodel.CoinDetailViewModel
import java.util.Locale

@Composable
fun CoinDetailScreen(
    coinId: String,
    viewModel: CoinDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(coinId) {
        viewModel.loadCoinDetail(coinId)
    }

    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CRTBlack)
                .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ">>> ASSET_DOSSIER: ${coinId.uppercase()}",
                color = WallStreetGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
            )

            if (uiState is CoinDetailUiState.Success) {
                val isTracked = (uiState as CoinDetailUiState.Success).detail.isTracked
                Text(
                    text = if (isTracked) "[UNTRACK]" else "[TRACK]",
                    color = if (isTracked) Color.Gray else WallStreetAmber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { viewModel.toggleTracking() },
                )
            }
        }
        HorizontalDivider(color = GridGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

        when (val state = uiState) {
            is CoinDetailUiState.Loading -> {
                TerminalLoadingSkeleton(Modifier.fillMaxSize())
            }
            is CoinDetailUiState.Success -> {
                CoinDetailContent(state.detail, state.ohlc)
            }
            is CoinDetailUiState.Error -> {
                TerminalErrorOverlay(message = state.message)
            }
        }
    }
}

@Composable
fun CoinDetailContent(
    detail: CoinDetail,
    ohlc: List<OHLCData>,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("OVERVIEW", "MARKETS", "HISTORICAL", "ABOUT")

    Column {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = WallStreetGreen,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = WallStreetGreen,
                )
            },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> OverviewTab(detail, ohlc)
            1 -> MarketsTab(detail)
            2 -> HistoricalTab(ohlc)
            3 -> AboutTab(detail)
        }
    }
}

@Composable
fun OverviewTab(detail: CoinDetail, ohlc: List<OHLCData>) {
    val colors = LocalTerminalColors.current
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        InfoRow("CURRENT_PRICE", "$${String.format(Locale.US, "%,.2f", detail.currentPrice)}")
        InfoRow("MARKET_CAP", "$${String.format(Locale.US, "%,.0f", detail.marketCap)}")
        InfoRow("24H_VOLUME", "$${String.format(Locale.US, "%,.0f", detail.totalVolume)}")
        InfoRow("24H_HIGH", "$${String.format(Locale.US, "%,.2f", detail.high24h)}")
        InfoRow("24H_LOW", "$${String.format(Locale.US, "%,.2f", detail.low24h)}")

        val changeColor = if (detail.priceChangePercentage24h >= 0) WallStreetGreen else WallStreetRed
        InfoRow("24H_CHANGE", "${String.format(Locale.US, "%.2f", detail.priceChangePercentage24h)}%", changeColor)

        Spacer(modifier = Modifier.height(16.dp))
        
        Text("--- 24H_PRICE_ACTION ---", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .border(1.dp, colors.grid, RectangleShape)
                    .padding(8.dp),
        ) {
            SimpleLineChart(
                data = ohlc.takeLast(24), 
                lineColor = if (detail.priceChangePercentage24h >= 0) colors.primary else colors.danger
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("7D_PERFORMANCE_SPARKLINE", color = TextGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .border(1.dp, GridGray, RectangleShape)
                    .padding(4.dp),
        ) {
            SimpleSparkline(
                prices = detail.sparkline,
                color = if ((detail.sparkline.lastOrNull() ?: 0.0) >= (detail.sparkline.firstOrNull() ?: 0.0)) WallStreetGreen else WallStreetRed
            )
        }
    }
}

@Composable
fun MarketsTab(detail: CoinDetail) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text("EXCHANGE", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1.5f), fontFamily = FontFamily.Monospace)
                Text("PAIR", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace)
                Text("PRICE", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace)
            }
        }
        items(detail.markets.take(20)) { ticker ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, GridGray)
                        .padding(8.dp),
            ) {
                Text(
                    ticker.exchange.uppercase(),
                    color = WallStreetAmber,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1.5f),
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    ticker.pair.uppercase(),
                    color = WallStreetGreen,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    "$${String.format(Locale.US, "%,.2f", ticker.price)}",
                    color = WallStreetGreen,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
fun HistoricalTab(ohlc: List<OHLCData>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text("DATE", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1.2f), fontFamily = FontFamily.Monospace)
                Text("OPEN", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace)
                Text("CLOSE", color = TextGray, fontSize = 10.sp, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace)
            }
        }
        items(ohlc.reversed()) { data ->
            val date = java.text.SimpleDateFormat("dd/MM/yy", Locale.US).format(java.util.Date(data.timestamp))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .border(0.5.dp, GridGray)
                        .padding(8.dp),
            ) {
                Text(date, color = WallStreetAmber, fontSize = 11.sp, modifier = Modifier.weight(1.2f), fontFamily = FontFamily.Monospace)
                Text(
                    String.format(Locale.US, "%.2f", data.open),
                    color = WallStreetGreen,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    String.format(Locale.US, "%.2f", data.close),
                    color = WallStreetGreen,
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f),
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
fun AboutTab(detail: CoinDetail) {
    // Intelligent paragraph splitting
    val paragraphs =
        remember(detail.description) {
            val clean =
                android.text.Html
                    .fromHtml(detail.description, android.text.Html.FROM_HTML_MODE_COMPACT)
                    .toString()
            // If text has no newlines, split every 3 sentences
            if (!clean.contains("\n\n")) {
                val sentences = clean.split(Regex("(?<=\\.)\\s+"))
                sentences.chunked(3).map { it.joinToString(" ") }
            } else {
                clean.split("\n\n").filter { it.isNotBlank() }
            }
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Text(
            text = "--- DECODED_ASSET_INTEL ---",
            color = WallStreetAmber,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        paragraphs.forEach { para ->
            Row(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = ">> ",
                    color = WallStreetAmber,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = para.trim(),
                    color = WallStreetGreen,
                    fontSize = 13.sp,
                    lineHeight = 22.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = GridGray.copy(alpha = 0.5f), thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "SOURCE_URL:",
                color = WallStreetAmber,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = detail.homepage.replace(Regex("https?://"), "").uppercase(),
                color = TextGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                textDecoration = TextDecoration.Underline,
            )
        }

        Text(
            text = "STATUS: DATA_INTEGRITY_VERIFIED",
            color = GridGray,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = WallStreetGreen,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "$label:", color = TextGray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = valueColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
