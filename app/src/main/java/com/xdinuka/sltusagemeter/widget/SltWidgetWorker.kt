package com.xdinuka.sltusagemeter.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.xdinuka.sltusagemeter.data.auth.ProfileStore
import com.xdinuka.sltusagemeter.data.model.UsageDetail
import com.xdinuka.sltusagemeter.data.model.UsageSummaryBundle
import com.xdinuka.sltusagemeter.data.repository.SltRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SltWidgetWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val repository: SltRepository,
    private val profileStore: ProfileStore
) : CoroutineWorker(context, params) {

    /**
     * In-memory dedup map for a single worker run.
     * Key: "$profileId/$telephoneNo"
     * Multiple widgets showing the same account share one API call, not N.
     */
    private val runCache = mutableMapOf<String, Pair<UsageSummaryBundle?, List<UsageDetail>>>()

    /** Profiles already resolved (accounts fetched) in this run. */
    private val resolvedProfiles = mutableSetOf<String>()

    override suspend fun doWork(): Result {
        val profiles = profileStore.profiles.value

        if (profiles.isEmpty()) {
            allGlanceIds(SltWidget::class.java).forEach { (widgetId, _) ->
                WidgetStateStore.save(context, widgetId, WidgetDisplayState.LoggedOut)
            }
            SltWidget().updateAll(context)
            return Result.success()
        }

        return try {
            runCache.clear()
            resolvedProfiles.clear()

            // ── 2×1 usage widgets ──────────────────────────────────────────
            allGlanceIds(SltWidget::class.java).forEach { (widgetId, _) ->
                val config = WidgetConfigStore(context).getConfig(widgetId)
                val profileId = config.profileId ?: profiles.firstOrNull()?.id ?: return@forEach
                val profile = resolveProfile(profileId) ?: return@forEach
                val telephoneNo = config.telephoneNo
                    ?: profile.telephoneNumbers.firstOrNull()
                    ?: return@forEach

                val (summary, vas) = fetchCached(profileId, telephoneNo)

                if (summary == null) {
                    WidgetStateStore.save(context, widgetId, WidgetDisplayState.Error("No data"))
                } else {
                    WidgetStateStore.save(
                        context, widgetId,
                        WidgetDisplayState.Success(
                            subscriberID = telephoneNo,
                            status = summary.status ?: "",
                            items = buildWidgetItems(summary, vas),
                            lastFetchedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            SltWidget().updateAll(context)

            // ── 1×1 metric widgets ─────────────────────────────────────────
            allGlanceIds(SltMetricWidget::class.java).forEach { (widgetId, _) ->
                val config = MetricWidgetConfigStore(context).getConfig(widgetId)
                val profileId = config.profileId ?: profiles.firstOrNull()?.id ?: return@forEach
                val profile = resolveProfile(profileId) ?: return@forEach
                val telephoneNo = config.telephoneNo
                    ?: profile.telephoneNumbers.firstOrNull()
                    ?: return@forEach

                val (summary, vas) = fetchCached(profileId, telephoneNo)
                val allItems = if (summary != null) buildWidgetItems(summary, vas) else emptyList()
                MetricWidgetStateStore.save(context, widgetId, resolveMetricItem(allItems, config.dataPoint))
            }
            SltMetricWidget().updateAll(context)

            Result.success()
        } catch (e: Exception) {
            allGlanceIds(SltWidget::class.java).forEach { (widgetId, _) ->
                WidgetStateStore.save(
                    context, widgetId,
                    WidgetDisplayState.Error(e.message ?: "Update failed")
                )
            }
            SltWidget().updateAll(context)
            Result.retry()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Fetches usage for a profile/phone, deduplicating within this worker run.
     * The first call hits the network; subsequent calls for the same key return
     * the cached result immediately — zero extra API calls per run.
     */
    private suspend fun fetchCached(
        profileId: String,
        telephoneNo: String
    ): Pair<UsageSummaryBundle?, List<UsageDetail>> {
        val key = "$profileId/$telephoneNo"
        runCache[key]?.let { return it }

        val summary = runCatching {
            repository.fetchUsageSummary(profileId, telephoneNo)
        }.getOrNull()
        val vas = runCatching {
            repository.fetchVasBundles(profileId, telephoneNo)
        }.getOrElse { emptyList() }

        val result = Pair(summary, vas)
        runCache[key] = result
        return result
    }

    /**
     * Returns the profile with telephone numbers populated.
     * If telephoneNumbers is empty, calls fetchAccounts() once per profile per run.
     */
    private suspend fun resolveProfile(
        profileId: String
    ): com.xdinuka.sltusagemeter.data.auth.AccountProfile? {
        val profile = profileStore.getProfile(profileId) ?: return null
        if (profile.telephoneNumbers.isNotEmpty() || profileId in resolvedProfiles) return profile
        runCatching { repository.fetchAccounts(profileId) }
        resolvedProfiles += profileId
        return profileStore.getProfile(profileId)
    }

    private fun resolveMetricItem(
        items: List<WidgetUsageItem>,
        dataPoint: String
    ): MetricWidgetState {
        if (items.isEmpty()) return MetricWidgetState.Error("No data")
        val item: WidgetUsageItem? = when {
            dataPoint.startsWith("MAIN_") -> {
                val idx = dataPoint.removePrefix("MAIN_").toIntOrNull() ?: 0
                items.filter { it.dataPointType == DataPointType.MAIN.name }.getOrNull(idx)
            }
            dataPoint.startsWith("VAS_") -> {
                val idx = dataPoint.removePrefix("VAS_").toIntOrNull() ?: 0
                items.filter { it.dataPointType == DataPointType.VAS.name }.getOrNull(idx)
            }
            else -> items.firstOrNull { it.dataPointType == dataPoint }
        }
        return if (item != null) MetricWidgetState.Success(
            name = item.name,
            percentage = item.percentage,
            used = item.used,
            limit = item.limit,
            volumeUnit = item.volumeUnit,
            colorType = item.colorType
        ) else MetricWidgetState.Error("Metric not found")
    }

    private suspend fun <T : androidx.glance.appwidget.GlanceAppWidget> allGlanceIds(
        cls: Class<T>
    ): List<Pair<Int, androidx.glance.GlanceId>> {
        val manager = GlanceAppWidgetManager(context)
        return manager.getGlanceIds(cls).map { glanceId ->
            Pair(manager.getAppWidgetId(glanceId), glanceId)
        }
    }
}
