package com.xdinuka.sltusagemeter.widget

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@JsonClass(generateAdapter = true)
data class WidgetUsageItem(
    val name: String,
    val used: String,
    val limit: String?,
    val remaining: String?,
    val percentage: Int,
    val volumeUnit: String,
    /** "blue" | "purple" | "orange" | "green" */
    val colorType: String,
    /** DataPointType name for filtering */
    val dataPointType: String = DataPointType.MAIN.name
)

sealed class WidgetDisplayState {
    val type: String
        get() = when (this) {
            is Loading -> "loading"
            is LoggedOut -> "logged_out"
            is Success -> "success"
            is Error -> "error"
        }
    data object Loading : WidgetDisplayState()
    data object LoggedOut : WidgetDisplayState()
    data class Success(
        val subscriberID: String,
        val status: String,
        val items: List<WidgetUsageItem>,
        val lastFetchedAt: Long? = null
    ) : WidgetDisplayState()
    data class Error(val message: String) : WidgetDisplayState()
}

object WidgetStateStore {
    private fun prefsName(widgetId: Int) = "slt_widget_state_$widgetId"
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val itemAdapter = moshi.adapter(WidgetUsageItem::class.java)

    fun save(context: Context, widgetId: Int, state: WidgetDisplayState) {
        val prefs = context.getSharedPreferences(prefsName(widgetId), Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("state", state.type)
            when (state) {
                is WidgetDisplayState.Success -> {
                    putString("subscriberID", state.subscriberID)
                    putString("status", state.status)
                    val json = "[${state.items.joinToString(",") { itemAdapter.toJson(it) }}]"
                    putString("items", json)
                    if (state.lastFetchedAt != null) putLong("lastFetchedAt", state.lastFetchedAt)
                    else remove("lastFetchedAt")
                }
                is WidgetDisplayState.Error -> putString("error", state.message)
                else -> {}
            }
            apply()
        }
    }

    fun load(context: Context, widgetId: Int): WidgetDisplayState {
        val prefs = context.getSharedPreferences(prefsName(widgetId), Context.MODE_PRIVATE)
        return when (prefs.getString("state", null)) {
            "success" -> {
                val subId = prefs.getString("subscriberID", "") ?: ""
                val status = prefs.getString("status", "") ?: ""
                val itemsJson = prefs.getString("items", "[]") ?: "[]"
                val items = parseItemsJson(itemsJson)
                val ts = if (prefs.contains("lastFetchedAt")) prefs.getLong("lastFetchedAt", 0) else null
                WidgetDisplayState.Success(subId, status, items, ts)
            }
            "error" -> WidgetDisplayState.Error(prefs.getString("error", "Unknown error") ?: "")
            "logged_out" -> WidgetDisplayState.LoggedOut
            else -> WidgetDisplayState.Loading
        }
    }

    /** Legacy single-widget load — used when widgetId is unknown. */
    fun load(context: Context): WidgetDisplayState = load(context, 0)

    /** Legacy single-widget save. */
    fun save(context: Context, state: WidgetDisplayState) = save(context, 0, state)

    private fun parseItemsJson(json: String): List<WidgetUsageItem> {
        return try {
            val raw = moshi.adapter(Any::class.java).fromJson(json)
            (raw as? List<*>)?.mapNotNull { item ->
                runCatching { itemAdapter.fromJson(moshi.adapter(Any::class.java).toJson(item)) }.getOrNull()
            } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }
}

// ─── Metric widget state ────────────────────────────────────────────────────

sealed class MetricWidgetState {
    data object Loading : MetricWidgetState()
    data object LoggedOut : MetricWidgetState()
    data class Success(
        val name: String,
        val percentage: Int,
        val used: String,
        val limit: String?,
        val volumeUnit: String,
        val colorType: String
    ) : MetricWidgetState()
    data class Error(val message: String) : MetricWidgetState()
}

object MetricWidgetStateStore {
    private fun prefsName(widgetId: Int) = "slt_metric_state_$widgetId"

    fun save(context: Context, widgetId: Int, state: MetricWidgetState) {
        val prefs = context.getSharedPreferences(prefsName(widgetId), Context.MODE_PRIVATE)
        prefs.edit().apply {
            val type = when (state) {
                is MetricWidgetState.Loading -> "loading"
                is MetricWidgetState.LoggedOut -> "logged_out"
                is MetricWidgetState.Success -> "success"
                is MetricWidgetState.Error -> "error"
            }
            putString("state", type)
            when (state) {
                is MetricWidgetState.Success -> {
                    putString("name", state.name)
                    putInt("percentage", state.percentage)
                    putString("used", state.used)
                    putString("limit", state.limit)
                    putString("volumeUnit", state.volumeUnit)
                    putString("colorType", state.colorType)
                }
                is MetricWidgetState.Error -> putString("error", state.message)
                else -> {}
            }
            apply()
        }
    }

    fun load(context: Context, widgetId: Int): MetricWidgetState {
        val prefs = context.getSharedPreferences(prefsName(widgetId), Context.MODE_PRIVATE)
        return when (prefs.getString("state", null)) {
            "success" -> MetricWidgetState.Success(
                name = prefs.getString("name", "") ?: "",
                percentage = prefs.getInt("percentage", 0),
                used = prefs.getString("used", "0") ?: "0",
                limit = prefs.getString("limit", null),
                volumeUnit = prefs.getString("volumeUnit", "GB") ?: "GB",
                colorType = prefs.getString("colorType", "blue") ?: "blue"
            )
            "error" -> MetricWidgetState.Error(prefs.getString("error", "") ?: "")
            "logged_out" -> MetricWidgetState.LoggedOut
            else -> MetricWidgetState.Loading
        }
    }
}
