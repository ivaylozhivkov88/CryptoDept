package com.cryptodept.ui.news

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

private fun safeOpenBrowser(context: Context, url: String) {
    if (url.isBlank()) {
        Log.e("TERMINAL_ERROR", "Uplink aborted: URL is empty")
        return
    }

    try {
        // Корекция на URL адреса за максимална съвместимост
        val formattedUrl = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("/") -> "https://cryptopanic.com$url"
            else -> "https://$url"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e("TERMINAL_ERROR", "Uplink failed: ${e.message}")
    }
}

@Composable
fun NewsScreen(viewModel: NewsViewModel) {
    val news by viewModel.news.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val context = LocalContext.current

    var selectedNews by remember { mutableStateOf<NewsItem?>(null) }
    val filters = listOf("ALL", "BITCOIN", "ETHEREUM", "SOLANA", "XRP", "ADA", "DOT", "SHIB")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CRTBlack)
                .padding(8.dp)
        ) {
            Text(
                text = "--- GLOBAL NEWS FEED [WIRE: CRYPTOPANIC] ---",
                color = WallStreetGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    Box(
                        modifier = Modifier
                            .border(1.dp, if (currentFilter == filter) WallStreetGreen else GridGray, RectangleShape)
                            .background(if (currentFilter == filter) WallStreetGreen.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { viewModel.setFilter(filter) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = filter, color = WallStreetGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            if (isLoading) {
                Text("LOADING WIRE DATA...", color = WallStreetGreen, fontFamily = FontFamily.Monospace)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(news) { item ->
                        NewsRow(item, onClick = { selectedNews = item })
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selectedNews != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            selectedNews?.let { item ->
                NewsDetailOverlay(
                    item = item,
                    onDismiss = { selectedNews = null },
                    onOpenUrl = { url -> safeOpenBrowser(context, url) }
                )
            }
        }
    }
}

@Composable
fun NewsRow(item: NewsItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WallStreetGreen.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = "[${item.source.uppercase()}]", color = WallStreetGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                text = item.sentiment ?: "NEUTRAL",
                color = if (item.sentiment == "BULLISH") WallStreetGreen else if (item.sentiment == "BEARISH") WallStreetRed else WallStreetGreen,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = item.title, color = WallStreetGreen, fontSize = 14.sp, lineHeight = 18.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "RECEIVED: ${item.publishedAt.take(16).replace("T", " ")}", color = TerminalGreen.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}

@Composable
fun NewsDetailOverlay(item: NewsItem, onDismiss: () -> Unit, onOpenUrl: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable { onDismiss() }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, WallStreetGreen)
                .background(CRTBlack)
                .padding(16.dp)
                .clickable(enabled = false) { }
        ) {
            Text(text = ">>> INCOMING DATA STREAM", color = WallStreetGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = WallStreetGreen, thickness = 1.dp)

            Text(
                text = item.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SOURCE: ${item.source}\n" +
                        "SENTIMENT: ${item.sentiment}\n" +
                        "TIMESTAMP: ${item.publishedAt.replace("T", " ")}\n" +
                        "STATUS: DECRYPTED\n\n" +
                        "Full information is available at the original source node. Establishing secure uplink...",
                color = WallStreetGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ГЛАВЕН БУТОН ЗА ЛИНКА
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenUrl(item.url) },
                color = WallStreetGreen
            ) {
                Text(
                    text = "ACCESS FULL DATA STREAM",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                border = BorderStroke(1.dp, WallStreetGreen)
            ) {
                Text("CLOSE OVERLAY", color = WallStreetGreen, fontFamily = FontFamily.Monospace)
            }
        }
    }
}