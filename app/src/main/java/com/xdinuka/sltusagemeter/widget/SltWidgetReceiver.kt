package com.xdinuka.sltusagemeter.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.xdinuka.sltusagemeter.data.prefs.AppPrefsStore
import java.util.concurrent.TimeUnit

class SltWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = SltWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        enqueueOneTimeWork(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        enqueueOneTimeWork(context)
        enqueuePeriodicWork(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    companion object {
        private const val ONE_TIME_WORK_NAME = "slt_widget_refresh"
        private const val PERIODIC_WORK_NAME = "slt_widget_periodic"

        fun enqueueOneTimeWork(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SltWidgetWorker>()
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            )
        }

        fun enqueuePeriodicWork(context: Context) {
            val intervalMinutes = context
                .getSharedPreferences(AppPrefsStore.PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(AppPrefsStore.KEY_REFRESH_INTERVAL, 30)
                .toLong()
                .coerceAtLeast(15)

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<SltWidgetWorker>(intervalMinutes, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
            )
        }
    }
}
