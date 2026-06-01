package com.cryptodept.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptodept.domain.model.CoinPrice
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.MarketsViewModel

@Composable
fun QuantAssetSelector(
    title: String,
    onAssetSelected: (String) -> Unit,
    onBack: () -> Unit,
    marketsViewModel: MarketsViewModel = hiltViewModel()
) {
    val colors = LocalTerminalColors.current
    val marketsState by marketsViewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">>> $title",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "[ CLOSE ]",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                modifier = Modifier.clickable { onBack() }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SEARCH BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, colors.primary, RectangleShape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text("FILTER_ASSETS...", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    }
                    innerTextField()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("QUANT_READY_ASSETS:", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)

        Spacer(modifier = Modifier.height(8.dp))

        if (marketsState is com.cryptodept.viewmodel.MarketsUiState.Success) {
            val allCoins = (marketsState as com.cryptodept.viewmodel.MarketsUiState.Success).coins
            
            // OPTIMIZATION: Show tracked coins first
            val filteredCoins = allCoins
                .filter { it.name.contains(searchQuery, true) || it.symbol.contains(searchQuery, true) }
                .sortedByDescending { it.isTracked }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCoins) { coin ->
                    QuantAssetRow(
                        coin = coin,
                        onClick = { onAssetSelected(coin.id) }
                    )
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        }
    }
}

@Composable
fun QuantAssetRow(
    coin: CoinPrice,
    onClick: () -> Unit
) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, colors.primary.copy(alpha = 0.3f), RectangleShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = coin.symbol.uppercase(),
                    color = colors.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (coin.isTracked) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "[TRACKED]",
                        color = colors.amber,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            Text(
                text = coin.name.uppercase(),
                color = colors.dimText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$${String.format(java.util.Locale.US, "%.2f", coin.currentPrice)}",
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "RUN_SCAN →",
                color = colors.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
