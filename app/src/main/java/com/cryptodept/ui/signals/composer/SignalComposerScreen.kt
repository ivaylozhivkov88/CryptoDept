package com.cryptodept.ui.signals.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    var indicator by remember { mutableStateOf(IndicatorType.RSI) }
    var operator by remember { mutableStateOf(ComparisonOperator.LESS_THAN) }
    var threshold by remember { mutableStateOf("30") }
    var action by remember { mutableStateOf(SignalAction.BUY) }
    
    // Task 2.12: Add notification toggles
    var soundEnabled by remember { mutableStateOf(true) }
    var vibeEnabled by remember { mutableStateOf(true) }
    var pushEnabled by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        modifier = Modifier.border(1.dp, colors.primary, RectangleShape),
        title = { Text("BUILD SIGNAL RULE", color = colors.primary, fontFamily = FontFamily.Monospace) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("RULE NAME", color = colors.dimText) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = colors.surface, unfocusedContainerColor = colors.surface),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("CONDITION:", color = colors.amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                var showIndicatorDropdown by remember { mutableStateOf(false) }
                var showOperatorDropdown by remember { mutableStateOf(false) }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Indicator Selection
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { showIndicatorDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(indicator.name, fontSize = 10.sp)
                        }
                        DropdownMenu(expanded = showIndicatorDropdown, onDismissRequest = { showIndicatorDropdown = false }) {
                            IndicatorType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = { indicator = type; showIndicatorDropdown = false }
                                )
                            }
                        }
                    }
                    
                    // Operator Selection
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { showOperatorDropdown = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(operator.name.replace("_", " "), fontSize = 10.sp)
                        }
                        DropdownMenu(expanded = showOperatorDropdown, onDismissRequest = { showOperatorDropdown = false }) {
                            ComparisonOperator.entries.forEach { op ->
                                DropdownMenuItem(
                                    text = { Text(op.name.replace("_", " ")) },
                                    onClick = { operator = op; showOperatorDropdown = false }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = { Text("THRESHOLD VALUE", color = colors.dimText) },
                    colors = TextFieldDefaults.colors(focusedContainerColor = colors.surface),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("ALERT_PROTOCOL:", color = colors.amber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                NotificationToggle("PUSH NOTIFICATIONS", pushEnabled) { pushEnabled = it }
                NotificationToggle("SOUND ALARM", soundEnabled) { soundEnabled = it }
                NotificationToggle("HAPTIC VIBRATION", vibeEnabled) { vibeEnabled = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = threshold.toDoubleOrNull() ?: 30.0
                val conditions = listOf(CustomSignalCondition(indicator, operator, value))
                onSave(name, conditions, LogicalOperator.AND, action)
            }) {
                Text("SAVE_AND_ACTIVATE", color = colors.primary, fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
fun NotificationToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalTerminalColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = colors.primary)
        )
    }
}
