package com.cryptodept.ui.tools

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.*
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.EntryAnalyzerViewModel
import com.cryptodept.viewmodel.EntryAnalysisUiState
import com.cryptodept.viewmodel.AnalysisComponent
import java.util.Locale

@Composable
fun EntryAnalyzerScreen(
    viewModel: EntryAnalyzerViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onUseInPlanner: (String, Double, Double, Double) -> Unit = { _, _, _, _ -> }
) {
    val colors = LocalTerminalColors.current
    val trackedCoins by viewModel.trackedCoins.collectAsState()
    val selectedCoin by viewModel.selectedCoin.collectAsState()
    val uiState by viewModel.entryData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
    ) {
        // 1. Header
        Text(
            text = ">>> OPTIMAL ENTRY CALCULATOR",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Coin selector row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(trackedCoins) { coin ->
                val isSelected = selectedCoin == coin
                Box(
                    modifier = Modifier
                        .border(1.dp, if (isSelected) colors.primary else colors.grid, RectangleShape)
                        .background(if (isSelected) colors.primary.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { viewModel.selectCoin(coin) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = coin.uppercase(),
                        color = if (isSelected) colors.primary else colors.dimText,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (val state = uiState) {
                is EntryAnalysisUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("ANALYZING MARKET DATA...", color = colors.primary, fontFamily = FontFamily.Monospace)
                    }
                }
                is EntryAnalysisUiState.Success -> {
                    EntryAnalyzerContent(
                        state = state,
                        colors = colors,
                        onUseInPlanner = onUseInPlanner,
                        onRefresh = { viewModel.refresh() }
                    )
                }
                is EntryAnalysisUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("[ERROR] ${state.message}", color = colors.danger, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("< BACK_TO_TOOLS", color = colors.primary, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun EntryAnalyzerContent(
    state: EntryAnalysisUiState.Success,
    colors: TerminalColorSet,
    onUseInPlanner: (String, Double, Double, Double) -> Unit,
    onRefresh: () -> Unit
) {
    val analysis = state.analysis
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // 4. REAL-TIME DATA панел
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val data = "RSI: [${String.format(Locale.US, "%.1f", state.currentRsi)}] | TREND: [▲]"
                Text(data, color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
        }

        // 6. ENTRY ANALYSIS резултати
        item {
            val scoreColor = when {
                analysis.entryScore > 70 -> colors.primary
                analysis.entryScore > 40 -> colors.amber
                else -> colors.danger
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, scoreColor)
                    .background(scoreColor.copy(alpha = 0.05f))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ENTRY SCORE", color = scoreColor, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(
                    text = "${analysis.entryScore}/100",
                    color = scoreColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = analysis.verdict.name.replace("_", " "),
                    color = scoreColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            state.components.forEach { component ->
                ComponentProgressBar(component, colors)
            }
            
            HorizontalDivider(color = colors.grid, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
        }

        // 7. IDENTIFIED ENTRY ZONES
        item {
            Text(">>> IDENTIFIED ENTRY ZONES", color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        if (analysis.betterZones.isEmpty()) {
            item {
                Text("NO OPTIMAL ZONES DETECTED", color = colors.dimText, fontSize = 11.sp)
            }
        } else {
            items(analysis.betterZones) { zone ->
                EntryZoneCard(zone)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // 8. ACTION buttons
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { 
                        val zone = analysis.betterZones.firstOrNull()
                        onUseInPlanner(analysis.coin, zone?.priceTo ?: analysis.currentPrice, zone?.priceFrom ?: (analysis.currentPrice * 0.95), analysis.currentPrice * 1.1)
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.background)
                ) {
                    Text("USE IN PLANNER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(0.5f).height(48.dp),
                    shape = RectangleShape,
                    border = BorderStroke(1.dp, colors.primary)
                ) {
                    Text("REFRESH", color = colors.primary, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun ComponentProgressBar(component: AnalysisComponent, colors: TerminalColorSet) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(component.name, color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text(component.verdict, color = colors.primary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        LinearProgressIndicator(
            progress = { component.value },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = colors.primary,
            trackColor = colors.grid,
            strokeCap = RectangleShape.let { androidx.compose.ui.graphics.StrokeCap.Butt }
        )
    }
}

@Composable
fun EntryZoneCard(zone: EntryZone) {
    val colors = LocalTerminalColors.current
    val color = Color(zone.type.color)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, color)
            .background(color.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(zone.type.label, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("${zone.confluenceCount} signals", color = color.copy(alpha = 0.7f), fontSize = 10.sp)
        }
        Text(
            text = "$${String.format(Locale.US, "%,.2f", zone.priceFrom)} - $${String.format(Locale.US, "%,.2f", zone.priceTo)}",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(zone.reason, color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}
