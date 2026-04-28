package com.cryptodept.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertsScreen() {
    val alerts = remember {
        listOf(
            AlertLog("BTC", "PRICE_SURGE", "+5.2% in 1h", System.currentTimeMillis() - 3600000),
            AlertLog("ETH", "RSI_OVERSOLD", "Value: 28.5", System.currentTimeMillis() - 7200000),
            AlertLog("SOL", "VOLUME_SPIKE", "3x avg volume", System.currentTimeMillis() - 10800000),
            AlertLog("BTC", "DEATH_CROSS", "SMA50 crossed SMA200", System.currentTimeMillis() - 86400000)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CRTBlack)
            .padding(16.dp)
    ) {
        Text(
            text = ">>> SYSTEM ALERT HISTORY",
            color = WallStreetGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(alerts) { alert ->
                AlertItem(alert)
            }
        }
    }
}

@Composable
fun AlertItem(alert: AlertLog) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    val color = if (alert.type == "PRICE_SURGE") WallStreetGreen else WallStreetAmber

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GridGray, RectangleShape)
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "[${alert.symbol}] ${alert.type}",
                color = color,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = sdf.format(Date(alert.timestamp)),
                color = TextGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "LOG: ${alert.message}",
            color = TerminalWhite.copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

data class AlertLog(
    val symbol: String,
    val type: String,
    val message: String,
    val timestamp: Long
)
