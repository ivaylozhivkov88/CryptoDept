package com.cryptodept.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryptodept.ui.theme.CryptoDeptTheme
import com.cryptodept.ui.theme.LocalTerminalColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED. This will cause the widget host to cancel out of the widget placement if they press the back button.
        setResult(RESULT_CANCELED)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId =
                extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID,
                )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            CryptoDeptTheme {
                WidgetConfigScreen(
                    onConfirm = {
                        val resultValue =
                            Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                        setResult(Activity.RESULT_OK, resultValue)
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
fun WidgetConfigScreen(onConfirm: () -> Unit) {
    val colors = LocalTerminalColors.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = ">>> WIDGET_INITIALIZATION",
            color = colors.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "SYNCING WITH DATA_STREAM...",
            color = colors.dimText,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onConfirm,
            colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
            shape = RectangleShape,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("[AUTHORIZE_DEPLOYMENT]", color = colors.primary, fontFamily = FontFamily.Monospace)
        }
    }
}
