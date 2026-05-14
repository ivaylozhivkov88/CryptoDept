package com.cryptodept.ui.prediction

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.MarketsViewModel

@Composable
fun PredictionHubScreen(
    onBack: () -> Unit,
    predictionViewModel: PredictionViewModel = hiltViewModel(),
    marketsViewModel: MarketsViewModel = hiltViewModel()
) {
    val colors = LocalTerminalColors.current
    val predictionState by predictionViewModel.uiState.collectAsStateWithLifecycle()
    val marketsState by marketsViewModel.uiState.collectAsStateWithLifecycle()
    
    var searchQuery by remember { mutableStateOf("") }
    
    BackHandler {
        if (predictionState !is PredictUiState.Idle) {
            predictionViewModel.reset()
        } else {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val state = predictionState) {
            is PredictUiState.Idle -> {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text(
                        text = ">>> PREDICT_CORE_HUB",
                        color = colors.primary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // SEARCH BAR
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .border(1.dp, colors.primary, RectangleShape)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                            cursorBrush = SolidColor(colors.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { /* Handled by filtering */ }),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("SEARCH_ASSET...", color = colors.dimText, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                                }
                                innerTextField()
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("SELECT_ASSET_FOR_QUANT_SCAN:", color = colors.dimText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // ASSET LIST
                    val filteredCoins = if (marketsState is com.cryptodept.viewmodel.MarketsUiState.Success) {
                        val allCoins = (marketsState as com.cryptodept.viewmodel.MarketsUiState.Success).coins
                        if (searchQuery.isEmpty()) allCoins.take(20)
                        else allCoins.filter { it.name.contains(searchQuery, true) || it.symbol.contains(searchQuery, true) }
                    } else emptyList()
                    
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredCoins) { coin ->
                            AssetPredictRow(
                                symbol = coin.symbol.uppercase(),
                                name = coin.name,
                                price = "$${String.format("%.2f", coin.currentPrice)}",
                                color = colors.primary,
                                onClick = { predictionViewModel.startDeepAnalysis(coin.id) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RectangleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary)
                    ) {
                        Text("< RETURN_TO_TERMINAL", color = colors.primary, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            is PredictUiState.Loading -> {
                AnalysisLoadingScreen(state)
            }
            is PredictUiState.Success -> {
                OracleResultScreen(
                    prediction = state.prediction,
                    modelVotes = state.prediction.ensembleConsensus.modelVotes,
                    onDismiss = { predictionViewModel.reset() }
                )
            }
            is PredictUiState.Error -> {
                TerminalPredictionError(message = state.message, onRetry = { predictionViewModel.reset() })
            }
        }
    }
}

@Composable
fun AssetPredictRow(
    symbol: String,
    name: String,
    price: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, color.copy(alpha = 0.3f), RectangleShape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(symbol, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
            Text(name.uppercase(), color = color.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Text(price, color = color, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Text("[ RUN_SCAN ]", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun TerminalPredictionError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("CRITICAL_SYSTEM_ERROR", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, color = Color.Red, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = Color.White),
            shape = RectangleShape
        ) {
            Text("RESET_CORE", fontFamily = FontFamily.Monospace)
        }
    }
}
