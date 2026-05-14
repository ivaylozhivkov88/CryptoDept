package com.cryptodept.ui.ai

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.AICoachViewModel
import com.cryptodept.viewmodel.ChatMessage

@Composable
fun AICoachScreen(
    viewModel: AICoachViewModel = hiltViewModel(),
    initialPrompt: String? = null
) {
    val colors = LocalTerminalColors.current
    val messages = viewModel.messages
    val isLoading by viewModel.isLoading.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            viewModel.setInputText(initialPrompt)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        Text(
            text = ">>> AI TRADE COACH [GEMINI]",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Actions
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickActionButton("ANALYZE JOURNAL") { viewModel.analyzeJournal() }
            QuickActionButton("DAILY ADVICE") { viewModel.sendMessage("GIVE ME DAILY TRADING ADVICE") }
            QuickActionButton("EVALUATE SETUP") { viewModel.sendMessage("HOW SHOULD I EVALUATE A NEW SETUP?") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chat History
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, colors.grid, RectangleShape)
                    .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (isLoading && (messages.isEmpty() || messages.last().sender == "YOU")) {
                item {
                    Text("COACH IS THINKING...", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, colors.primary, RectangleShape)
                    .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("YOU> ", color = colors.amber, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
            BasicTextField(
                value = inputText,
                onValueChange = { viewModel.setInputText(it) },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions =
                    KeyboardActions(onSend = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            viewModel.setInputText("")
                        }
                    }),
            )
        }
    }
}

@Composable
fun ChatBubble(msg: ChatMessage) {
    val colors = LocalTerminalColors.current
    val isCoach = msg.sender == "COACH"
    val borderColor = if (isCoach) colors.primary else colors.amber
    val prefix = if (isCoach) "COACH> " else "YOU> "

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor.copy(alpha = 0.5f), RectangleShape)
                .padding(8.dp),
    ) {
        Text(
            text = prefix + msg.text,
            color = if (isCoach) colors.primary else colors.textPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
fun QuickActionButton(
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    Box(
        modifier =
            Modifier
                .border(1.dp, colors.primary, RectangleShape)
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = colors.primary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}
