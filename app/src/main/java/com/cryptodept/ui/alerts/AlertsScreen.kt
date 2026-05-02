package com.cryptodept.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.Alert
import com.cryptodept.domain.model.AlertDirection
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.AlertsViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val alerts by viewModel.alerts.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CRTBlack)
            .padding(16.dp)
    ) {
        Text(
            text = ">>> ACTIVE PRICE ALERTS",
            color = WallStreetGreen,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, GridGray, RectangleShape),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = ">>> NO ACTIVE ALERTS FOUND\n>>> STANDBY_MODE",
                    color = TextGray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
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
}

@Composable
fun AlertItem(alert: Alert) {
    val color = if (alert.direction == AlertDirection.ABOVE) WallStreetGreen else WallStreetRed

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
                text = "[${alert.coinSymbol.uppercase()}]",
                color = WallStreetAmber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = if (alert.isActive) "ACTIVE" else "TRIGGERED",
                color = if (alert.isActive) WallStreetGreen else TextGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "TRIGGER: ${if (alert.direction == AlertDirection.ABOVE) "ABOVE" else "BELOW"} $${String.format(Locale.US, "%,.2f", alert.targetPrice)}",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}
