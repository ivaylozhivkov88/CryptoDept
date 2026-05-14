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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.CalendarEvent
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.CalendarViewModel
import java.util.*

@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val events by viewModel.filteredEvents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val colors = LocalTerminalColors.current

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .border(1.dp, colors.grid, RectangleShape),
        ) {
            HeaderSection(onHotToggle = { viewModel.toggleHotOnly() })

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(">>> NO UPCOMING EVENTS FOUND", color = colors.dimText, fontFamily = FontFamily.Monospace)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(events) { event ->
                        CalendarEventItem(event)
                        HorizontalDivider(color = colors.grid.copy(alpha = 0.2f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(onHotToggle: () -> Unit) {
    val colors = LocalTerminalColors.current
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
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Text(
            text = "[HOT ONLY]",
            color = colors.dimText,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.clickable { onHotToggle() },
        )
    }
    HorizontalDivider(color = colors.grid, thickness = 1.dp)
}

@Composable
fun CalendarEventItem(event: CalendarEvent) {
    val colors = LocalTerminalColors.current
    val dayLabel =
        when (event.daysUntil) {
            0 -> "🔥 TODAY"
            1 -> "IN 1 DAY"
            else -> "IN ${event.daysUntil} DAYS"
        }

    Column(modifier = Modifier.padding(12.dp)) {
        Text(
            text = dayLabel,
            color = if (event.daysUntil <= 1) colors.error else colors.amber,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${event.coins.joinToString(", ")} — ${event.title}",
            color = colors.textPrimary,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Category: ${event.category}",
                color = colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Text(
                text = "Hot Score: ${String.format(Locale.US, "%.1f", event.hotScore)}",
                color = if (event.isHot) colors.primary else colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }
    }
}
