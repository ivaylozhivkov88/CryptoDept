package com.cryptodept.ui.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.cryptodept.domain.manager.AchievementEngine
import com.cryptodept.domain.model.Achievement
import com.cryptodept.ui.theme.LocalTerminalColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AchievementsScreen(
    engine: AchievementEngine,
    onBack: () -> Unit,
) {
    val colors = LocalTerminalColors.current
    val achievements by engine.achievements.collectAsState()
    val unlockedCount = achievements.count { it.unlockedAt != null }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(16.dp),
    ) {
        // Header
        Text(
            text = ">>> OPERATOR_ACHIEVEMENTS_LOG",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "STATUS: ",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
            Text(
                text = "$unlockedCount/${achievements.size} DEPLOYED",
                color = colors.amber,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress Bar
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .border(1.dp, colors.grid, RectangleShape),
        ) {
            val progress = if (achievements.isNotEmpty()) unlockedCount.toFloat() / achievements.size else 0f
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(colors.primary.copy(alpha = 0.3f)),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(achievements) { achievement ->
                AchievementItem(achievement)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
            shape = RectangleShape,
        ) {
            Text("[RETURN_TO_COMMAND_CENTER]", color = colors.primary, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun AchievementItem(achievement: Achievement) {
    val colors = LocalTerminalColors.current
    val isUnlocked = achievement.unlockedAt != null
    val borderColor = if (isUnlocked) colors.primary else colors.grid.copy(alpha = 0.3f)
    val textColor = if (isUnlocked) colors.primary else colors.dimText

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RectangleShape)
                .background(if (isUnlocked) colors.surface.copy(alpha = 0.1f) else Color.Transparent)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (isUnlocked) achievement.icon else "🔒",
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 12.dp),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = achievement.title,
                color = textColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Text(
                text = achievement.description,
                color = textColor.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
        }

        if (isUnlocked) {
            val date = SimpleDateFormat("dd.MM.yy", Locale.US).format(Date(achievement.unlockedAt!!))
            Text(
                text = "[$date]",
                color = colors.amber,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
    }
}
