package com.cryptodept.ui.news

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.cryptodept.domain.model.NewsItem
import com.cryptodept.domain.model.NewsSentiment
import com.cryptodept.ui.components.EmptyState
import com.cryptodept.ui.components.ErrorState
import com.cryptodept.ui.components.skeletons.NewsCardSkeleton
import com.cryptodept.ui.theme.*
import com.cryptodept.util.TerminalConfig
import java.util.*

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import com.cryptodept.ui.components.TerminalCard

@Composable
fun NewsScreen(viewModel: NewsViewModel) {
    val news by viewModel.news.collectAsStateWithLifecycle()
    val pagingNews = viewModel.pagingNews.collectAsLazyPagingItems()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val colors = LocalTerminalColors.current
    val context = LocalContext.current

    val filters = listOf("ALL", "FAVORITES", "BITCOIN", "ETHEREUM", "BULLISH", "BEARISH")

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(TerminalConfig.UI.DEFAULT_PADDING),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">>> INTELLIGENCE_WIRE",
                color = colors.primary,
                fontSize = TerminalConfig.UI.FONT_SIZE_HEADER,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = colors.primary)
            }
        }

        // HORIZONTAL FILTERS (REVOLUT STYLE)
        LazyRow(
            contentPadding = PaddingValues(horizontal = TerminalConfig.UI.DEFAULT_PADDING),
            horizontalArrangement = Arrangement.spacedBy(TerminalConfig.UI.SPACER_MEDIUM),
            modifier = Modifier.fillMaxWidth().padding(bottom = TerminalConfig.UI.SPACER_LARGE),
        ) {
            items(filters) { filter ->
                val isSelected = currentFilter == filter
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) colors.primary.copy(alpha = 0.15f) else colors.surface)
                            .border(TerminalConfig.UI.BORDER_WIDTH, if (isSelected) colors.primary else colors.grid, RoundedCornerShape(20.dp))
                            .clickable { viewModel.setFilter(filter) }
                            .padding(horizontal = TerminalConfig.UI.DEFAULT_PADDING, vertical = TerminalConfig.UI.SMALL_PADDING),
                ) {
                    Text(
                        text = filter,
                        color = if (isSelected) colors.primary else colors.dimText,
                        fontSize = TerminalConfig.UI.FONT_SIZE_SMALL,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        when {
            isLoading && pagingNews.itemCount == 0 -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(TerminalConfig.UI.DEFAULT_PADDING)) {
                    items(5) { NewsCardSkeleton() }
                }
            }
            error != null -> {
                ErrorState(message = error!!, onRetry = { viewModel.refresh() }, modifier = Modifier.fillMaxSize())
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(TerminalConfig.UI.DEFAULT_PADDING),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // TOP STORIES SECTION
                    if (currentFilter == "ALL" && pagingNews.itemCount > 0) {
                        item {
                            Text("TOP_STORIES", color = colors.dimText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(5) { index ->
                                    pagingNews[index]?.let { item ->
                                        NewsHeadlineCard(item) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("PREVIOUS_TRANSMISSIONS", color = colors.dimText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (currentFilter == "ALL") {
                        items(pagingNews.itemCount) { index ->
                            pagingNews[index]?.let { item ->
                                NewsListItem(item) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                                }
                            }
                        }
                    } else {
                        items(news) { item ->
                            NewsListItem(item) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewsHeadlineCard(item: NewsItem, onClick: () -> Unit) {
    val colors = LocalTerminalColors.current
    Card(
        modifier = Modifier.width(220.dp).height(280.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.imageUrl ?: "https://cryptodept.com/placeholder.png",
                contentDescription = null,
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            // Gradient Overlay
            Box(
                modifier = Modifier.fillMaxSize().background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                    )
                )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)
            ) {
                Text(
                    text = item.currencies.firstOrNull()?.uppercase() ?: "MARKET",
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = item.source, color = colors.dimText, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun NewsListItem(item: NewsItem, onClick: () -> Unit) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.currencies.firstOrNull()?.uppercase() ?: "SYS",
                    color = colors.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = remember(item.publishedAt) {
                        val diff = System.currentTimeMillis() - item.publishedAt
                        val hours = diff / (60 * 60 * 1000)
                        if (hours < 1) "${diff / (60 * 1000)}m" else "${hours}h"
                    },
                    color = colors.dimText,
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.title,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = item.source, color = colors.dimText, fontSize = 11.sp)
        }
        
        AsyncImage(
            model = item.imageUrl ?: "https://cryptodept.com/placeholder.png",
            contentDescription = null,
            modifier = Modifier.size(70.dp).clip(RoundedCornerShape(8.dp)).background(colors.surface),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    }
}
