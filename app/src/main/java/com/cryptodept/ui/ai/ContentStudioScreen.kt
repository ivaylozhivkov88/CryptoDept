package com.cryptodept.ui.ai

import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.ui.components.TerminalCard
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.ContentCategory
import com.cryptodept.viewmodel.ContentStudioViewModel

@Composable
fun ContentStudioScreen(
    viewModel: ContentStudioViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToAiCoach: (initialPrompt: String) -> Unit
) {
    val colors = LocalTerminalColors.current
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val context = LocalContext.current

    // Handle pending navigation
    LaunchedEffect(uiState.pendingNavigationToAiCoach) {
        uiState.pendingNavigationToAiCoach?.let { prompt ->
            onNavigateToAiCoach(prompt)
            viewModel.navigationConsumed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("<", color = colors.primary, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            Text(">>> CONTENT_FACTORY_V2", color = colors.primary, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 1. Scope Selection
        TerminalCard(title = "SELECT SOURCE") {
            Row(modifier = Modifier.fillMaxWidth()) {
                ScopeButton(
                    text = "GLOBAL MARKET",
                    isSelected = uiState.isGlobalScope,
                    modifier = Modifier.weight(1f)
                ) { viewModel.setScope(true) }
                
                Spacer(Modifier.width(8.dp))
                
                ScopeButton(
                    text = "SPECIFIC COIN",
                    isSelected = !uiState.isGlobalScope,
                    modifier = Modifier.weight(1f)
                ) { viewModel.setScope(false) }
            }

            if (!uiState.isGlobalScope) {
                Spacer(Modifier.height(16.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(favorites) { coin ->
                        FilterChip(
                            selected = uiState.selectedCoinId == coin.id,
                            onClick = { viewModel.setSelectedCoin(coin.id) },
                            label = { Text(coin.symbol.uppercase(), fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = colors.background,
                                containerColor = colors.grid.copy(alpha = 0.2f),
                                labelColor = colors.textPrimary
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Generation Actions
        Text("SELECT OUTPUT TYPE", color = colors.dimText, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionCard(
                title = "NARRATIVE",
                icon = Icons.Default.Description,
                desc = "Text Post",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.generateContent(ContentCategory.TEXT) }
            )
            ActionCard(
                title = "VISUAL",
                icon = Icons.Default.BarChart,
                desc = "Infographic",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.generateContent(ContentCategory.CHART) }
            )
            ActionCard(
                title = "MOTION",
                icon = Icons.Default.MovieFilter,
                desc = "AI Video",
                modifier = Modifier.weight(1f),
                onClick = { viewModel.generateContent(ContentCategory.VIDEO) }
            )
        }

        // 3. Output Area
        if (uiState.isLoading || uiState.generatedOutput.isNotBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            TerminalCard(title = "GENERATED_OUTPUT [${uiState.lastGeneratedType}]") {
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = colors.primary)
                }
                
                SelectionContainer {
                    Text(
                        text = uiState.generatedOutput,
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }

                if (uiState.generatedOutput.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, uiState.generatedOutput)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export Content"))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = colors.background, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("SHARE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.sendPromptToAiCoach(uiState.generatedOutput) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                            border = BorderStroke(1.dp, colors.primary),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("AI COACH", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScopeButton(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalTerminalColors.current
    Box(
        modifier = modifier
            .clickable { onClick() }
            .border(1.dp, if (isSelected) colors.primary else colors.grid)
            .background(if (isSelected) colors.primary.copy(alpha = 0.1f) else Color.Transparent)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) colors.primary else colors.dimText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ActionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = LocalTerminalColors.current
    Column(
        modifier = modifier
            .clickable { onClick() }
            .border(1.dp, colors.grid)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(desc, color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
