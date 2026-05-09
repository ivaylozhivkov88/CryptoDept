package com.cryptodept.ui.analysis.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.data.db.PriceHistoryEntity
import com.cryptodept.ui.theme.GridGray
import com.cryptodept.ui.theme.WallStreetGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoricalTab(history: List<PriceHistoryEntity>) {
    val dateFormatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Text("DATE/TIME", modifier = Modifier.weight(1f), color = WallStreetGreen, fontSize = 10.sp)
            Text("PRICE", modifier = Modifier.weight(1f), color = WallStreetGreen, fontSize = 10.sp)
        }

        HorizontalDivider(color = GridGray)

        LazyColumn {
            items(history.sortedByDescending { it.timestamp }) { entry ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(
                        text = dateFormatter.format(Date(entry.timestamp)),
                        modifier = Modifier.weight(1f),
                        color = WallStreetGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", entry.price)}",
                        modifier = Modifier.weight(1f),
                        color = WallStreetGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                }
                HorizontalDivider(color = GridGray.copy(alpha = 0.3f))
            }
        }
    }
}
