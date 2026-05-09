package com.cryptodept.ui.ai

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
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
import com.cryptodept.domain.model.AudienceProfile
import com.cryptodept.ui.components.TerminalCard
import com.cryptodept.ui.components.TerminalInput
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.ContentStudioUiState
import com.cryptodept.viewmodel.ContentStudioViewModel
import com.cryptodept.viewmodel.ContentTemplate

@Composable
fun ContentStudioScreen(
    viewModel: ContentStudioViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    val uiState by viewModel.uiState.collectAsState()
    val generatedPrompt by viewModel.generatedPrompt.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()

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
            Text("GENERATE PROMPT", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        if (generatedPrompt.isNotBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            TerminalCard(title = "GENERATED PROMPT") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(8.dp)
                            .border(1.dp, colors.dimText),
                ) {
                    Text(generatedPrompt, color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.sendToAi() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RectangleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.amber),
                ) {
                    Text("SEND TO AI COACH", color = Color.Black)
                }
            }
        }

        if (aiResponse.isNotBlank() || uiState is ContentStudioUiState.Loading) {
            Spacer(modifier = Modifier.height(24.dp))
            TerminalCard(title = "AI OUTPUT") {
                if (uiState is ContentStudioUiState.Loading) {
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
}
