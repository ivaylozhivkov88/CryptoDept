package com.cryptodept.ui.ai

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.AudienceProfile
import com.cryptodept.ui.components.TerminalCard
import com.cryptodept.ui.components.TerminalInput
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.ContentStudioViewModel
import com.cryptodept.viewmodel.ContentTemplate

@Composable
fun ContentStudioScreen(
    viewModel: ContentStudioViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToAiCoach: (initialPrompt: String) -> Unit
) {
    val colors = LocalTerminalColors.current
    val uiState by viewModel.uiState.collectAsState()
    val generatedPrompt by viewModel.generatedPrompt.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var lastCopyTime by remember { mutableStateOf(0L) }
    var showAgentDialog by remember { mutableStateOf(false) }

    // Handle pending navigation
    LaunchedEffect(uiState.pendingNavigationToAiCoach) {
        uiState.pendingNavigationToAiCoach?.let { prompt ->
            onNavigateToAiCoach(prompt)
            viewModel.navigationConsumed()
        }
    }

    var selectedTemplate by remember { mutableStateOf(ContentTemplate.DAILY_RECAP) }
    var selectedAudience by remember { mutableStateOf(AudienceProfile.DAY_TRADER) }

    // Dynamic parameters depending on template
    var asset by remember { mutableStateOf("BTC") }
    var headline by remember { mutableStateOf("CRYPTO SHOCK") }
    var priceTarget by remember { mutableStateOf("$100,000") }
    var coinB by remember { mutableStateOf("ETH") }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("<", color = colors.primary, modifier = Modifier.clickable { onBack() }.padding(8.dp))
            Text(
                ">>> CONTENT GENERATION STUDIO",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TerminalCard(title = "1. SELECT TEMPLATE") {
            ContentTemplate.entries.forEach { template ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedTemplate = template }
                            .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedTemplate == template,
                        onClick = { selectedTemplate = template },
                        colors = RadioButtonDefaults.colors(selectedColor = colors.primary, unselectedColor = colors.dimText),
                    )
                    Text(template.label, color = if (selectedTemplate == template) colors.primary else colors.textPrimary, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TerminalCard(title = "2. TARGET AUDIENCE") {
            AudienceProfile.entries.forEach { audience ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { selectedAudience = audience }
                            .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedAudience == audience,
                        onClick = { selectedAudience = audience },
                        colors = RadioButtonDefaults.colors(selectedColor = colors.primary, unselectedColor = colors.dimText),
                    )
                    Text(audience.label, color = if (selectedAudience == audience) colors.primary else colors.textPrimary, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TerminalCard(title = "3. PARAMETERS") {
            TerminalInput(label = "MAIN ASSET (Symbol)", value = asset, onValueChange = { asset = it })

            when (selectedTemplate) {
                ContentTemplate.THUMBNAIL -> {
                    TerminalInput(label = "HEADLINE", value = headline, onValueChange = { headline = it })
                }
                ContentTemplate.WHAT_IF -> {
                    TerminalInput(label = "PRICE TARGET", value = priceTarget, onValueChange = { priceTarget = it })
                }
                ContentTemplate.COMPARISON -> {
                    TerminalInput(label = "COMPARE WITH", value = coinB, onValueChange = { coinB = it })
                }
                else -> {
                    TerminalInput(label = "TOPIC / CONTEXT", value = headline, onValueChange = { headline = it })
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val params =
                    mapOf(
                        "asset" to asset,
                        "headline" to headline,
                        "topic" to headline,
                        "price" to priceTarget,
                        "coinA" to asset,
                        "coinB" to coinB,
                    )
                viewModel.generatePrompt(selectedTemplate, selectedAudience, params)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
            shape = RectangleShape,
        ) {
            Text("GENERATE PROMPT", color = colors.background, fontWeight = FontWeight.Bold)
        }

        if (generatedPrompt.isNotBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            TerminalCard(title = "GENERATED PROMPT") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(colors.background)
                            .padding(8.dp)
                            .border(1.dp, colors.dimText),
                ) {
                    Text(generatedPrompt, color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                
                // Bonus 2: Character Counter
                Text(
                    text = "Characters: ${generatedPrompt.length} | Words: ${generatedPrompt.split("\\s+".toRegex()).size}",
                    color = colors.dimText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // COPY TEXT button
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastCopyTime > 1500) { // Debounce edge case 4
                                clipboardManager.setText(AnnotatedString(generatedPrompt))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                Toast.makeText(
                                    context,
                                    ">>> PROMPT_COPIED_TO_CLIPBOARD",
                                    Toast.LENGTH_SHORT
                                ).show()
                                lastCopyTime = now
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.background
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "COPY TEXT",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // SEND TO AI COACH button
                    OutlinedButton(
                        onClick = {
                            // Copy to clipboard first
                            clipboardManager.setText(AnnotatedString(generatedPrompt))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showAgentDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.primary
                        ),
                        border = BorderStroke(1.dp, colors.primary),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "SEND TO AI",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Bonus 1: Share Intent Button
                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, generatedPrompt)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share prompt via"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share, 
                            contentDescription = "Share",
                            tint = colors.primary
                        )
                    }
                }
            }
        }

        if (aiResponse.isNotBlank() || uiState.isLoading) {
            Spacer(modifier = Modifier.height(24.dp))
            TerminalCard(title = "AI OUTPUT") {
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = colors.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                SelectionContainer {
                    Text(aiResponse, color = colors.textPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showAgentDialog) {
        AlertDialog(
            onDismissRequest = { showAgentDialog = false },
            containerColor = colors.background,
            modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
            title = {
                Text(
                    ">>> SELECT_PROCESSOR_UNIT",
                    color = colors.primary,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column {
                    Text(
                        "PROMPT_COPIED. Select which agent should process this task:",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    AgentSelectRow("AI_STRATEGIC_COACH", "Gemini 1.5 Pro | Expert Analysis") {
                        showAgentDialog = false
                        viewModel.sendPromptToAiCoach(generatedPrompt)
                    }
                    
                    AgentSelectRow("MARKETING_STRATEGIST", "Viral Post Engine | Direct Generate") {
                        showAgentDialog = false
                        viewModel.sendToAi()
                    }
                    
                    AgentSelectRow("NARRATIVE_ORCHESTRATOR", "Auto-Synthesis | Coming Soon", enabled = false) {}
                }
            },
            confirmButton = {
                TextButton(onClick = { showAgentDialog = false }) {
                    Text("CANCEL", color = colors.dimText, fontFamily = FontFamily.Monospace)
                }
            }
        )
    }
}

@Composable
fun AgentSelectRow(
    name: String,
    desc: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalTerminalColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp)
            .border(
                width = 0.5.dp, 
                color = if (enabled) colors.grid else colors.grid.copy(alpha = 0.3f),
                shape = RectangleShape
            )
            .padding(12.dp)
    ) {
        Text(
            text = "[$name]",
            color = if (enabled) colors.primary else colors.dimText,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = desc,
            color = if (enabled) colors.textPrimary.copy(alpha = 0.7f) else colors.dimText.copy(alpha = 0.5f),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}
