package com.xdinuka.sltusagemeter.widget

import com.xdinuka.sltusagemeter.data.model.UsageDetail
import com.xdinuka.sltusagemeter.data.model.UsageSummaryBundle

/**
 * Converts raw API models into a flat list of [WidgetUsageItem]s.
 * Shared by [SltWidgetWorker] (refresh) and the config activities (live preview of names).
 */
internal fun buildWidgetItems(
    summary: UsageSummaryBundle,
    vas: List<UsageDetail>
): List<WidgetUsageItem> = buildList {
    summary.myPackageInfo?.usageDetails?.forEach { detail ->
        add(WidgetUsageItem(
            name        = detail.name,
            used        = detail.used ?: "0",
            limit       = detail.limit,
            remaining   = detail.remaining,
            percentage  = detail.percentage,
            volumeUnit  = detail.volumeUnit ?: "GB",
            colorType   = "blue",
            dataPointType = DataPointType.MAIN.name
        ))
    }
    summary.bonusDataSummary?.let { bonus ->
        val usedVal  = (bonus.used  ?: "0").toFloatOrNull() ?: 0f
        val limitVal = (bonus.limit ?: "0").toFloatOrNull() ?: 1f
        add(WidgetUsageItem(
            name        = "Bonus Data",
            used        = bonus.used ?: "0",
            limit       = bonus.limit,
            remaining   = "%.2f".format((limitVal - usedVal).coerceAtLeast(0f)),
            percentage  = if (limitVal > 0) ((usedVal / limitVal) * 100).toInt() else 0,
            volumeUnit  = bonus.volumeUnit ?: "GB",
            colorType   = "purple",
            dataPointType = DataPointType.BONUS.name
        ))
    }
    summary.extraGbDataSummary?.let { extra ->
        val usedVal  = (extra.used  ?: "0").toFloatOrNull() ?: 0f
        val limitVal = (extra.limit ?: "0").toFloatOrNull() ?: 1f
        add(WidgetUsageItem(
            name        = "Extra GB",
            used        = extra.used ?: "0",
            limit       = extra.limit,
            remaining   = "%.2f".format((limitVal - usedVal).coerceAtLeast(0f)),
            percentage  = if (limitVal > 0) ((usedVal / limitVal) * 100).toInt() else 0,
            volumeUnit  = extra.volumeUnit ?: "GB",
            colorType   = "orange",
            dataPointType = DataPointType.EXTRA_GB.name
        ))
    }
    vas.forEach { v ->
        add(WidgetUsageItem(
            name        = v.name,
            used        = v.used ?: "0",
            limit       = v.limit,
            remaining   = v.remaining,
            percentage  = v.percentage,
            volumeUnit  = v.volumeUnit ?: "GB",
            colorType   = "green",
            dataPointType = DataPointType.VAS.name
        ))
    }
}

/** Derives picker options from the actual item names returned by the API. */
internal fun metricOptionsFromItems(items: List<WidgetUsageItem>): List<Pair<String, String>> =
    buildList {
        items.filter { it.dataPointType == DataPointType.MAIN.name }
            .forEachIndexed { i, it -> add("MAIN_$i" to it.name) }
        items.firstOrNull { it.dataPointType == DataPointType.BONUS.name }
            ?.let { add("BONUS" to it.name) }
        items.firstOrNull { it.dataPointType == DataPointType.EXTRA_GB.name }
            ?.let { add("EXTRA_GB" to it.name) }
        items.filter { it.dataPointType == DataPointType.VAS.name }
            .forEachIndexed { i, it -> add("VAS_$i" to it.name) }
    }

/** Generic fallback labels shown before any data has been fetched. */
internal val GENERIC_METRIC_OPTIONS = listOf(
    "MAIN_0"  to "Main Package",
    "MAIN_1"  to "Main Package — 2",
    "BONUS"   to "Bonus Data",
    "EXTRA_GB" to "Extra GB",
    "VAS_0"   to "Add-on Bundle",
    "VAS_1"   to "Add-on Bundle — 2"
)
