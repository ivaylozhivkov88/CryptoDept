package com.cryptodept.ui.news

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.*
import com.cryptodept.domain.model.NewsItem
import com.cryptodept.domain.model.NewsSentiment
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NewsScreen(viewModel: NewsViewModel) {
    val news by viewModel.news.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val colors = LocalTerminalColors.current
    val context = LocalContext.current

    val filters = listOf("ALL", "BITCOIN", "ETHEREUM", "BULLISH", "BEARISH")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        Text(
            text = ">>> CRYPTO NEWS FEED",
            color = colors.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { filter ->
                val isSelected = currentFilter == filter
                Box(
                    modifier = Modifier
                        .border(1.dp, if (isSelected) colors.primary else colors.grid, RectangleShape)
                        .background(if (isSelected) colors.primary.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { viewModel.setFilter(filter) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) colors.primary else colors.dimText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("FETCHING WIRE DATA...", color = colors.primary, fontFamily = FontFamily.Monospace)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(news) { item ->
                    NewsCard(item) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.url))
                        context.startActivity(intent)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.refresh() },
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
        ) {
            Text("[REFRESH FEED]", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NewsCard(item: NewsItem, onOpen: () -> Unit) {
    val colors = LocalTerminalColors.current
    val sentimentColor = when (item.sentiment) {
        NewsSentiment.BULLISH -> colors.primary
        NewsSentiment.BEARISH -> colors.danger
        NewsSentiment.NEUTRAL -> colors.amber
    }
    
    val timeStr = remember(item.publishedAt) {
        val diff = System.currentTimeMillis() - item.publishedAt
        val mins = diff / (60 * 1000)
        if (mins < 60) "${mins}m" else "${mins / 60}h"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.grid, RectangleShape)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "[${item.sentiment.name.take(4)}]",
                    color = sentimentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.currencies.take(3).joinToString(", "),
                    color = colors.textPrimary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(text = timeStr, color = colors.dimText, fontSize = 10.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = item.title,
            color = colors.primary,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Source: ${item.source}",
                color = colors.dimText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            
            Text(
                text = "[OPEN ↗]",
                color = colors.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onOpen() }
            )
        }
    }
}
