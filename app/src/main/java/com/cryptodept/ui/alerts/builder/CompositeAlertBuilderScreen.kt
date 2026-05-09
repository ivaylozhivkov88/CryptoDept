package com.cryptodept.ui.alerts.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.AlertsViewModel
import kotlinx.collections.immutable.toImmutableList

@Composable
fun CompositeAlertBuilderScreen(
    onBack: () -> Unit,
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val colors = LocalTerminalColors.current

    var name by remember { mutableStateOf("PRO_STRATEGY_1") }
    var coinId by remember { mutableStateOf("bitcoin") }
    var symbol by remember { mutableStateOf("BTC") }
    var logicOperator by remember { mutableStateOf(AlertLogicOperator.AND) }
    val conditions = remember { mutableStateListOf<AlertCondition>() }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        Text(
            text = ">>> COMPOSITE_ALERT_BUILDER",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Basic Config
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("ALERT_NAME", color = colors.dimText) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(color = colors.primary, fontFamily = FontFamily.Monospace),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.grid,
                ),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LOGIC: ", color = colors.dimText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = "[${logicOperator.name}]",
                color = colors.amber,
                modifier =
                    Modifier.clickable {
                        logicOperator = if (logicOperator == AlertLogicOperator.AND) AlertLogicOperator.OR else AlertLogicOperator.AND
                    },
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Conditions Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("CONDITIONS:", color = colors.primary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Button(
                onClick = {
                    conditions.add(
                        AlertCondition(
                            type = AlertConditionType.PRICE,
                            operator = AlertDirection.ABOVE,
                            targetValue = 0.0,
                            description = "New condition",
                        ),
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary.copy(alpha = 0.1f)),
                shape = RectangleShape,
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = colors.primary)
                Text("ADD", color = colors.primary, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(conditions) { cond ->
                ConditionEditorItem(cond, onDelete = { conditions.remove(cond) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(
            onClick = {
                val alert =
                    CompositeAlert(
                        name = name,
                        coinId = coinId,
                        coinSymbol = symbol,
                        conditions = conditions.toImmutableList(),
                        logicOperator = logicOperator,
                    )
                viewModel.addCompositeAlert(alert)
                onBack()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
        ) {
            Text("DEPLOY ALERT DAEMON", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun ConditionEditorItem(
    condition: AlertCondition,
    onDelete: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    var type by remember { mutableStateOf(condition.type) }
    var operator by remember { mutableStateOf(condition.operator) }
    var value by remember { mutableStateOf(condition.targetValue.toString()) }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(1.dp, colors.grid)
                .padding(8.dp),
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "IF ${type.name}", color = colors.amber, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = colors.danger)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = operator.name,
                    color = colors.primary,
                    fontSize = 11.sp,
                    modifier =
                        Modifier.clickable {
                            operator = if (operator == AlertDirection.ABOVE) AlertDirection.BELOW else AlertDirection.ABOVE
                        },
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    textStyle = LocalTextStyle.current.copy(color = colors.primary, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.width(100.dp).border(0.5.dp, colors.grid).padding(4.dp),
                )
            }
        }
    }
}
