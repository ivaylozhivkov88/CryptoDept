package com.cryptodept.ui.signals.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.*
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun SignalComposerScreen(
    onBack: () -> Unit,
    viewModel: SignalComposerViewModel = hiltViewModel(),
) {
    val colors = LocalTerminalColors.current
    val rules by viewModel.rules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, conds, op, action ->
                viewModel.saveRule(name, conds, op, action)
                showAddDialog = false
            },
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ">>> SIGNAL_COMPOSER_V1",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onBack) {
                Text("[X]", color = colors.danger, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary.copy(alpha = 0.1f)),
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = colors.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("CREATE NEW SIGNAL RULE", color = colors.primary, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("ACTIVE RULES:", color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(rules) { rule ->
                RuleCard(rule, onToggle = { viewModel.toggleRule(rule.id, it) }, onDelete = { viewModel.deleteRule(rule.id) })
            }
        }
    }
}

@Composable
fun RuleCard(
    rule: CustomSignalRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.grid)
                .padding(12.dp),
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(rule.name.uppercase(), color = colors.primary, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Row {
                    Switch(
                        checked = rule.isActive,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.primary),
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = colors.danger, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Text(
                text = "LOGIC: ${rule.operator.name} | ACTION: ${rule.action.name}",
                color = colors.amber,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(modifier = Modifier.height(4.dp))
            rule.conditions.forEach { cond ->
                Text(
                    text = "> IF ${cond.indicator.name} ${cond.operator.name.replace("_", " ")} ${cond.value}",
                    color = colors.dimText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onSave: (String, List<CustomSignalCondition>, LogicalOperator, SignalAction) -> Unit,
) {
    val colors = LocalTerminalColors.current
    var name by remember { mutableStateOf("") }
    var action by remember { mutableStateOf(SignalAction.BUY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
        title = { Text("BUILD SIGNAL RULE", color = colors.primary, fontFamily = FontFamily.Monospace) },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("RULE NAME", color = colors.dimText) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = colors.surface, unfocusedContainerColor = colors.surface),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("CONDITION: RSI < 30 (STUB)", color = colors.dimText, fontSize = 10.sp)
                // In a real app, here we would have a more complex condition builder
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val conditions = listOf(CustomSignalCondition(IndicatorType.RSI, ComparisonOperator.LESS_THAN, 30.0))
                onSave(name, conditions, LogicalOperator.AND, action)
            }) {
                Text("SAVE", color = colors.primary)
            }
        },
    )
}
