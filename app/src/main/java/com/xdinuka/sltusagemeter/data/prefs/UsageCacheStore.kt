package com.xdinuka.sltusagemeter.data.prefs

import android.content.Context
import com.squareup.moshi.Moshi
import com.xdinuka.sltusagemeter.data.model.UsageDetail
import com.xdinuka.sltusagemeter.data.model.UsageSummaryBundle
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists usage data per (profileId, telephoneNo) so the app can show
 * last-known values without hitting the network on every open.
 *
 * Stores two separate JSON blobs keyed by profile+phone:
 *  - "<key>_summary"   → UsageSummaryBundle JSON
 *  - "<key>_vas"       → JSON array of UsageDetail
 *  - "<key>_ts"        → epoch-millis of the last successful fetch
 *
 * Using individual keys avoids wrapping in a container class, which would
 * require a second Moshi adapter and is the common source of runtime crashes.
 */
@Singleton
class UsageCacheStore @Inject constructor(
    @ApplicationContext private val context: Context,
    /** App-wide Moshi instance — guaranteed to have adapters for all API model types. */
    private val moshi: Moshi
) {
    private val prefs by lazy {
        context.getSharedPreferences("usage_cache", Context.MODE_PRIVATE)
    }

    private fun summaryKey(profileId: String, phone: String) = "${profileId}_${phone}_summary"
    private fun vasKey(profileId: String, phone: String)     = "${profileId}_${phone}_vas"
    private fun tsKey(profileId: String, phone: String)      = "${profileId}_${phone}_ts"

    fun save(
        profileId: String,
        telephoneNo: String,
        summary: UsageSummaryBundle?,
        vasBundles: List<UsageDetail>
    ) {
        runCatching {
            val summaryJson = summary?.let {
                moshi.adapter(UsageSummaryBundle::class.java).toJson(it)
            }
            @Suppress("UNCHECKED_CAST")
            val vasAdapter = moshi.adapter(List::class.java) as? com.squareup.moshi.JsonAdapter<List<UsageDetail>>
            val vasJson = runCatching { vasAdapter?.toJson(vasBundles) }.getOrNull()

            prefs.edit().apply {
                if (summaryJson != null) putString(summaryKey(profileId, telephoneNo), summaryJson)
                else remove(summaryKey(profileId, telephoneNo))
                if (vasJson != null) putString(vasKey(profileId, telephoneNo), vasJson)
                putLong(tsKey(profileId, telephoneNo), System.currentTimeMillis())
                apply()
            }
        }
    }

    fun load(profileId: String, telephoneNo: String): UsageCacheEntry? {
        val ts = if (prefs.contains(tsKey(profileId, telephoneNo)))
            prefs.getLong(tsKey(profileId, telephoneNo), 0L)
        else return null   // never saved

        val summary = runCatching {
            prefs.getString(summaryKey(profileId, telephoneNo), null)
                ?.let { moshi.adapter(UsageSummaryBundle::class.java).fromJson(it) }
        }.getOrNull()

        val vasBundles = runCatching {
            val json = prefs.getString(vasKey(profileId, telephoneNo), null) ?: return@runCatching emptyList()
            @Suppress("UNCHECKED_CAST")
            val adapter = moshi.adapter(List::class.java) as? com.squareup.moshi.JsonAdapter<List<*>>
            val raw = adapter?.fromJson(json) ?: emptyList<Any?>()
            // Re-serialize each element through the UsageDetail adapter
            val detailAdapter = moshi.adapter(UsageDetail::class.java)
            val anyAdapter = moshi.adapter(Any::class.java)
            raw.mapNotNull { element ->
                runCatching { detailAdapter.fromJson(anyAdapter.toJson(element)) }.getOrNull()
            }
        }.getOrElse { emptyList() }

        return UsageCacheEntry(lastFetchedAt = ts, summary = summary, vasBundles = vasBundles)
    }

    fun clear(profileId: String, telephoneNo: String) {
        prefs.edit().apply {
            remove(summaryKey(profileId, telephoneNo))
            remove(vasKey(profileId, telephoneNo))
            remove(tsKey(profileId, telephoneNo))
            apply()
        }
    }
}

data class UsageCacheEntry(
    val lastFetchedAt: Long,
    val summary: UsageSummaryBundle?,
    val vasBundles: List<UsageDetail> = emptyList()
)
