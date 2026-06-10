package com.xdinuka.sltusagemeter.widget

import com.squareup.moshi.JsonClass

/** Theme applied to a widget instance independent of system/app theme. */
enum class WidgetThemeMode { SYSTEM, LIGHT, DARK }

/** Named data point categories that can be toggled per-widget. */
enum class DataPointType(val label: String) {
    MAIN("Main Package"),
    BONUS("Bonus Data"),
    EXTRA_GB("Extra GB"),
    VAS("Add-on Bundles")
}

/** Hex colours available in the widget colour picker. */
val WIDGET_COLOR_PALETTE = listOf(
    "#2196F3", "#03A9F4", "#00BCD4", "#4CAF50",
    "#8BC34A", "#009688", "#9C27B0", "#E91E63",
    "#FF9800", "#FF5722", "#F44336", "#607D8B"
)

/** Fallback colour used when no config is stored for a data-point type. */
val DEFAULT_DATA_POINT_COLORS: Map<String, String> = mapOf(
    DataPointType.MAIN.name     to "#2196F3",
    DataPointType.BONUS.name    to "#9C27B0",
    DataPointType.EXTRA_GB.name to "#FF9800",
    DataPointType.VAS.name      to "#4CAF50"
)

/**
 * Per-widget configuration stored in SharedPreferences, keyed by AppWidget ID.
 * All fields have safe defaults so the widget renders without explicit setup.
 */
@JsonClass(generateAdapter = true)
data class WidgetConfig(
    val widgetId: Int = 0,
    /** null = use first available profile */
    val profileId: String? = null,
    /** null = use first telephone number of the profile */
    val telephoneNo: String? = null,
    /** "SYSTEM" | "LIGHT" | "DARK" */
    val themeMode: String = WidgetThemeMode.SYSTEM.name,
    /** 0.0 (transparent) … 1.0 (opaque) */
    val backgroundAlpha: Float = 1.0f,
    /** Whether to show the subscriber ID in the widget header */
    val showAccountDetails: Boolean = true,
    /** Enabled data-point type names from [DataPointType] */
    val enabledDataPoints: List<String> = DataPointType.entries.map { it.name },
    /**
     * Per–data-point hex colour overrides.
     * Missing keys fall back to [DEFAULT_DATA_POINT_COLORS].
     */
    val dataPointColors: Map<String, String> = DEFAULT_DATA_POINT_COLORS,
    /** Whether to show the "Updated X ago" timestamp line at the bottom of the widget. */
    val showLastFetched: Boolean = true
) {
    /** Returns the configured hex colour for [dataPointType], falling back to the default. */
    fun colorFor(dataPointType: String): String =
        dataPointColors[dataPointType]
            ?: DEFAULT_DATA_POINT_COLORS[dataPointType]
            ?: "#2196F3"
}

/**
 * Per-widget configuration for the 1×1 metric widget.
 */
@JsonClass(generateAdapter = true)
data class MetricWidgetConfig(
    val widgetId: Int = 0,
    val profileId: String? = null,
    val telephoneNo: String? = null,
    /**
     * Which single data point to show.
     * Format: DataPointType name + optional index, e.g. "MAIN_0", "MAIN_1", "BONUS", "EXTRA_GB", "VAS_0".
     */
    val dataPoint: String = "MAIN_0",
    val themeMode: String = WidgetThemeMode.SYSTEM.name,
    val backgroundAlpha: Float = 1.0f,
    /** Hex colour of the progress arc */
    val color: String = "#2196F3"
)
