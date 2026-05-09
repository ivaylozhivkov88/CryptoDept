package com.cryptodept.ui.components.skeletons

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cryptodept.ui.theme.LocalTerminalColors

@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    val colors = LocalTerminalColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "skeleton_shimmer",
    )
    val shimmerColor = colors.grid.copy(alpha = alpha)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(12.dp),
    ) {
        // TickerTape skeleton
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(shimmerColor)
                    .padding(bottom = 12.dp),
        )

        // Header section (3 rows)
        repeat(3) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                        .padding(bottom = 8.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(shimmerColor),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier =
                        Modifier
                            .weight(0.5f)
                            .fillMaxHeight()
                            .background(shimmerColor),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Market list (8 rows)
        repeat(8) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(colors.background)
                        .border(1.dp, colors.grid)
                        .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Symbol placeholder
                Box(
                    modifier =
                        Modifier
                            .width(48.dp)
                            .fillMaxHeight()
                            .background(shimmerColor),
                )

                // Price placeholder
                Box(
                    modifier =
                        Modifier
                            .width(72.dp)
                            .fillMaxHeight()
                            .background(shimmerColor),
                )

                // Change % placeholder
                Box(
                    modifier =
                        Modifier
                            .width(48.dp)
                            .fillMaxHeight()
                            .background(shimmerColor),
                )
            }
            if (it < 7) Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun ChartsSkeleton(modifier: Modifier = Modifier) {
    val colors = LocalTerminalColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "chart_skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "chart_shimmer",
    )
    val shimmerColor = colors.grid.copy(alpha = alpha)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(12.dp),
    ) {
        // Coin info placeholders
        repeat(2) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(shimmerColor)
                        .padding(bottom = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chart area placeholder
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(colors.background)
                    .border(1.dp, colors.grid),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Indicators row
        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(4) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(shimmerColor),
                )
            }
        }
    }
}

@Composable
fun AnalysisSkeleton(modifier: Modifier = Modifier) {
    val colors = LocalTerminalColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "analysis_skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "analysis_shimmer",
    )
    val shimmerColor = colors.grid.copy(alpha = alpha)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(12.dp),
    ) {
        // Risk meter circle placeholder
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .background(shimmerColor)
                    .padding(bottom = 16.dp),
        )

        // Metric cards (4)
        repeat(4) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(colors.background)
                        .border(1.dp, colors.grid)
                        .padding(8.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(shimmerColor),
                )
            }
            if (it < 3) Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun JournalSkeleton(modifier: Modifier = Modifier) {
    val colors = LocalTerminalColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "journal_skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "journal_shimmer",
    )
    val shimmerColor = colors.grid.copy(alpha = alpha)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(12.dp),
    ) {
        // Summary cards row
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(4) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(60.dp)
                            .background(shimmerColor),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Trade rows (5)
        repeat(5) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(colors.background)
                        .border(1.dp, colors.grid)
                        .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(shimmerColor),
                )
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(shimmerColor),
                )
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(shimmerColor),
                )
            }
            if (it < 4) Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
