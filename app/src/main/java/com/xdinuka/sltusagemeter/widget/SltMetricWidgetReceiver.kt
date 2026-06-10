package com.xdinuka.sltusagemeter.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class SltMetricWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SltMetricWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Piggyback on the main widget worker — it also refreshes metric widgets
        SltWidgetReceiver.enqueueOneTimeWork(context)
    }
}
