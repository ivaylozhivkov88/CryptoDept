package com.cryptodept.ui.screensaver

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.LocalTerminalColors
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun BloombergWallScreen(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val colors = LocalTerminalColors.current
    val symbols = listOf(
        "BTC", "ETH", "SOL", "XRP", "ADA", "DOT", "LINK", "LTC", "AVAX", "TRX",
        "MATIC", "SHIB", "UNI", "BCH", "LEO", "NEAR", "ATOM", "STX", "XLM", "CRO"
    )
    
    val prices = remember { mutableStateMapOf<String, Double>() }
    val changes = remember { mutableStateMapOf<String, Double>() }

    LaunchedEffect(Unit) {
        symbols.forEach { 
            prices[it] = 50000.0 + Random().nextDouble() * 10000.0
            changes[it] = (Random().nextDouble() - 0.5) * 5.0
        }
        
        while (true) {
            delay(2000)
            val randomSymbol = symbols.random()
            prices[randomSymbol] = (prices[randomSymbol] ?: 0.0) * (1.0 + (Random().nextDouble() - 0.5) * 0.002)
            changes[randomSymbol] = (Random().nextDouble() - 0.5) * 5.0
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onDismiss() }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BLOOMBERG TERMINAL - LIVE MARKET FEED",
                    color = colors.amber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                val currentTime = remember { mutableStateOf(Calendar.getInstance().time) }
                LaunchedEffect(Unit) {
                    while(true) {
                        currentTime.value = Calendar.getInstance().time
                        delay(1000)
                    }
                }
                Text(
                    text = String.format(Locale.US, "%tT", currentTime.value),
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(symbols) { symbol ->
                    BloombergTickerItem(
                        symbol = symbol,
                        price = prices[symbol] ?: 0.0,
                        change = changes[symbol] ?: 0.0
                    )
                }
            }
        }
    }
}

@Composable
fun BloombergTickerItem(symbol: String, price: Double, change: Double) {
    val colors = LocalTerminalColors.current
    val changeColor = if (change >= 0) colors.primary else colors.danger
    val arrow = if (change >= 0) "▲" else "▼"

    Column(
        modifier = Modifier
            .background(Color.DarkGray.copy(alpha = 0.2f))
            .padding(8.dp)
    ) {
        Text(
            text = symbol,
            color = colors.amber,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = String.format(Locale.US, "$%,.2f", price),
            color = Color.White,
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = String.format(Locale.US, "%s %.2f%%", arrow, change),
            color = changeColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    }
}
