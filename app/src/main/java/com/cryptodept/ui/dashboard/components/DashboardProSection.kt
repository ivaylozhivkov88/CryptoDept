package com.cryptodept.ui.dashboard.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cryptodept.domain.model.LiquidationSummary

@Composable
fun DashboardProSection(
    liquidationSummary: LiquidationSummary?
) {
    Column {
        LiquidationHeatmapStrip(
            summary = liquidationSummary
        )
        Spacer(Modifier.height(4.dp))
    }
}
