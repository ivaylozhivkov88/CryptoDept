package com.cryptodept.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll

class CryptoDeptWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CryptoDeptWidget()

    /**
     * Forces all instances of the widget to update.
     */
    suspend fun forceUpdate(context: Context) {
        glanceAppWidget.updateAll(context)
    }
}
