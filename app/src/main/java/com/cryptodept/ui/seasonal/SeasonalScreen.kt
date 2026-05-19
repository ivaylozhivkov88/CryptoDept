package com.cryptodept.ui.seasonal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cryptodept.ui.theme.LocalTerminalColors
import com.cryptodept.viewmodel.SeasonalViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SeasonalScreen(
    onBack: () -> Unit,
    viewModel: SeasonalViewModel = hiltViewModel(),
) {
    val colors = LocalTerminalColors.current
    val cycleInfo by viewModel.cycleInfo.collectAsState()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ">>> BITCOIN_HALVING",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp, // Slightly smaller
                fontWeight = FontWeight.Bold,
            )
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.height(30.dp),
                shape = RectangleShape,
                border = BorderStroke(1.dp, colors.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text("RETURN", color = colors.primary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }

        HorizontalDivider(color = colors.grid, modifier = Modifier.padding(vertical = 12.dp))

        // CYCLE PROGRESS CARD
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, colors.primary)
                    .padding(16.dp),
        ) {
            Column {
                Text(
                    "CURRENT CYCLE: #${cycleInfo.cycleNumber}",
                    color = colors.dimText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = cycleInfo.currentPhase.label,
                    color = colors.primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // PROGRESS BAR
                Box(modifier = Modifier.fillMaxWidth().height(24.dp).border(1.dp, colors.grid)) {
                    LinearProgressIndicator(
                        progress = { cycleInfo.progressToNextHalving },
                        modifier = Modifier.fillMaxSize(),
                        color = colors.primary,
                        trackColor = Color.Transparent,
                    )
                    Text(
                        text = "${(cycleInfo.progressToNextHalving * 100).toInt()}% TO NEXT HALVING",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InfoItem("LAST HALVING", dateFormat.format(Date(cycleInfo.halvingDate)))
                    InfoItem("DAYS SINCE", "${cycleInfo.daysSinceHalving}d")
                    InfoItem("EST. NEXT", dateFormat.format(Date(cycleInfo.estimatedNextHalving)))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PHASE DESCRIPTION
        Text(">>> PHASE_STRATEGY_PROTOCOL", color = colors.primary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().background(colors.surface).padding(12.dp)) {
            Text(
                text = cycleInfo.currentPhase.description,
                color = colors.textPrimary,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 18.sp,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // HISTORICAL CONTEXT
        Text(">>> HISTORICAL_HALVING_DATA", color = colors.primary, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(12.dp))

        HistoricalRow("2012", "Cycle 1", "+9,000% gain post-halving")
        HistoricalRow("2016", "Cycle 2", "+3,000% gain post-halving")
        HistoricalRow("2020", "Cycle 3", "+600% gain post-halving")
        HistoricalRow("2024", "Cycle 4", "Current Cycle - In Progress")
    }
}

@Composable
fun InfoItem(
    label: String,
    value: String,
) {
    val colors = LocalTerminalColors.current
    Column {
        Text(label, color = colors.dimText, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = colors.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun HistoricalRow(
    year: String,
    label: String,
    result: String,
) {
    val colors = LocalTerminalColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(0.5.dp, colors.grid)
                .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(year, color = colors.amber, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text(label, color = colors.dimText, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(result, color = colors.primary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}
