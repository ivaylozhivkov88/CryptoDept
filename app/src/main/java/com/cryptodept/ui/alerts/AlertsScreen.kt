package com.cryptodept.ui.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.domain.model.Alert
import com.cryptodept.domain.model.AlertDirection
import com.cryptodept.ui.tutorial.tutorialTarget
import com.cryptodept.domain.tutorial.TutorialTargetId
import com.cryptodept.ui.theme.*
import com.cryptodept.viewmodel.AlertsViewModel
import java.util.*

@Composable
fun AlertsScreen(
    onNavigateToBuilder: () -> Unit,
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val alerts by viewModel.alerts.collectAsState()
    val compositeAlerts by viewModel.compositeAlerts.collectAsState()
    val colors = LocalTerminalColors.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToBuilder,
                containerColor = colors.primary,
                contentColor = Color.Black,
                shape = RectangleShape,
                modifier = Modifier.tutorialTarget(TutorialTargetId.ALERTS_COMPOSITE_BUILDER)
            ) {
                Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
        ) {
            Text(
                text = ">>> ACTIVE_ALERT_DAEMONS",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (alerts.isEmpty() && compositeAlerts.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .border(1.dp, colors.grid, RectangleShape),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    Text(
                        text = ">>> NO ACTIVE ALERTS FOUND\n>>> SCANNING_MODE: IDLE",
                        color = colors.dimText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .tutorialTarget(TutorialTargetId.ALERTS_LIST),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(alerts, key = { it.id }) { alert ->
                        SwipeToDeleteWrapper(onDelete = { viewModel.deleteAlert(alert.id) }) {
                            AlertItem(alert)
                        }
                    }
                    items(compositeAlerts, key = { it.id }) { alert ->
                        SwipeToDeleteWrapper(onDelete = { /* TODO: viewModel.deleteCompositeAlert(alert.id) */ }) {
                            CompositeAlertItem(alert)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteWrapper(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = {
                if (it == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                    true
                } else {
                    false
                }
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color =
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    LocalTerminalColors.current.danger
                } else {
                    Color.Transparent
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Black,
                )
            }
        },
        modifier = Modifier.testTag("SwipeToDelete"),
    ) {
        content()
    }
}

@Composable
fun AlertItem(alert: Alert) {
    val colors = LocalTerminalColors.current
    val color = if (alert.direction == AlertDirection.ABOVE) colors.primary else colors.danger

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.grid, RectangleShape)
                .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "[${alert.coinSymbol.uppercase()}]",
                color = colors.amber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                text = if (alert.isActive) "ACTIVE" else "TRIGGERED",
                color = if (alert.isActive) colors.primary else colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "TRIGGER: ${if (alert.direction == AlertDirection.ABOVE) "ABOVE" else "BELOW"} $${String.format(
                Locale.US,
                "%,.2f",
                alert.targetPrice,
            )}",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )
    }
}

@Composable
fun CompositeAlertItem(alert: com.cryptodept.domain.model.CompositeAlert) {
    val colors = LocalTerminalColors.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, colors.grid, RectangleShape)
                .padding(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "[${alert.name.uppercase()}]",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                text = if (alert.isActive) "ACTIVE" else "TRIGGERED",
                color = if (alert.isActive) colors.primary else colors.dimText,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
        Text(
            text = "TARGET: ${alert.coinSymbol.uppercase()} | LOGIC: ${alert.logicOperator}",
            color = colors.amber,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(modifier = Modifier.height(4.dp))
        alert.conditions.forEach { cond ->
            Text(
                text = "> IF ${cond.type} ${cond.operator} ${cond.targetValue}",
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
