package com.xdinuka.sltusagemeter.widget

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

private const val PREFS_WIDGET_CONFIG = "slt_widget_configs"
private const val PREFS_METRIC_CONFIG = "slt_metric_widget_configs"

class WidgetConfigStore(private val context: Context) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(WidgetConfig::class.java)

    fun getConfig(widgetId: Int): WidgetConfig {
        val prefs = context.getSharedPreferences(PREFS_WIDGET_CONFIG, Context.MODE_PRIVATE)
        val json = prefs.getString("cfg_$widgetId", null) ?: return WidgetConfig(widgetId = widgetId)
        return runCatching { adapter.fromJson(json) }.getOrNull() ?: WidgetConfig(widgetId = widgetId)
    }

    fun saveConfig(config: WidgetConfig) {
        context.getSharedPreferences(PREFS_WIDGET_CONFIG, Context.MODE_PRIVATE)
            .edit().putString("cfg_${config.widgetId}", adapter.toJson(config)).apply()
    }

    fun removeConfig(widgetId: Int) {
        context.getSharedPreferences(PREFS_WIDGET_CONFIG, Context.MODE_PRIVATE)
            .edit().remove("cfg_$widgetId").apply()
    }
}

class MetricWidgetConfigStore(private val context: Context) {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(MetricWidgetConfig::class.java)

    fun getConfig(widgetId: Int): MetricWidgetConfig {
        val prefs = context.getSharedPreferences(PREFS_METRIC_CONFIG, Context.MODE_PRIVATE)
        val json = prefs.getString("cfg_$widgetId", null) ?: return MetricWidgetConfig(widgetId = widgetId)
        return runCatching { adapter.fromJson(json) }.getOrNull() ?: MetricWidgetConfig(widgetId = widgetId)
    }

    fun saveConfig(config: MetricWidgetConfig) {
        context.getSharedPreferences(PREFS_METRIC_CONFIG, Context.MODE_PRIVATE)
            .edit().putString("cfg_${config.widgetId}", adapter.toJson(config)).apply()
    }

    fun removeConfig(widgetId: Int) {
        context.getSharedPreferences(PREFS_METRIC_CONFIG, Context.MODE_PRIVATE)
            .edit().remove("cfg_$widgetId").apply()
    }
}
