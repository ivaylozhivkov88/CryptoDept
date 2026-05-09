package com.cryptodept.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.CalendarEvent
import com.cryptodept.viewmodel.CalendarViewModel
import java.util.*

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val events by viewModel.filteredEvents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .border(1.dp, Color(0xFF00FF41), RectangleShape),
        ) {
            HeaderSection(onHotToggle = { viewModel.toggleHotOnly() })

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF00FF41))
                }
            } else if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(">>> NO UPCOMING EVENTS FOUND", color = Color.Gray, fontFamily = com.cryptodept.ui.theme.JetBrainsMono)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(events) { event ->
                        CalendarEventItem(event)
                        HorizontalDivider(color = Color(0xFF00FF41).copy(alpha = 0.2f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(onHotToggle: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ">>> CRYPTO CALENDAR",
            color = Color(0xFF00FF41),
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Text(
            text = "[HOT ONLY]",
            color = Color.Gray,
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontSize = 12.sp,
            modifier = Modifier.clickable { onHotToggle() },
        )
    }
    HorizontalDivider(color = Color(0xFF00FF41), thickness = 1.dp)
}

@Composable
fun CalendarEventItem(event: CalendarEvent) {
    val dayLabel =
        when (event.daysUntil) {
            0 -> "🔥 TODAY"
            1 -> "IN 1 DAY"
            else -> "IN ${event.daysUntil} DAYS"
        }

    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = dayLabel,
            color = if (event.daysUntil <= 1) Color(0xFFFF3B30) else Color(0xFFFFB000),
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${event.coins.joinToString(", ")} — ${event.title}",
            color = Color.White,
            fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
        Row(modifier = Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(
                text = "Category: ${event.category}",
                color = Color.Gray,
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                fontSize = 11.sp,
            )
            Text(
                text = "Hot Score: ${String.format(Locale.US, "%.1f", event.hotScore)}",
                color = if (event.isHot) Color(0xFF00FF41) else Color.Gray,
                fontFamily = com.cryptodept.ui.theme.JetBrainsMono,
                fontSize = 11.sp,
            )
        }
    }
}
