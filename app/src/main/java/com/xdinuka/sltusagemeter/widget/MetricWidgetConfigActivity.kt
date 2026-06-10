package com.xdinuka.sltusagemeter.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xdinuka.sltusagemeter.data.auth.AccountProfile
import com.xdinuka.sltusagemeter.data.auth.ProfileStore
import com.xdinuka.sltusagemeter.data.prefs.UsageCacheEntry
import com.xdinuka.sltusagemeter.data.prefs.UsageCacheStore
import com.xdinuka.sltusagemeter.ui.theme.SltTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MetricWidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var profileStore: ProfileStore
    @Inject lateinit var usageCacheStore: UsageCacheStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED); finish(); return
        }
        setResult(RESULT_CANCELED)

        setContent {
            SltTheme {
                MetricConfigScreen(
                    widgetId  = appWidgetId,
                    profiles  = profileStore.profiles.value,
                    existing  = MetricWidgetConfigStore(this).getConfig(appWidgetId),
                    loadCache = { profileId, phone -> usageCacheStore.load(profileId, phone) },
                    onSave = { config ->
                        MetricWidgetConfigStore(this).saveConfig(config)
                        SltWidgetReceiver.enqueueOneTimeWork(this)
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                        finish()
                    },
                    onCancel = { setResult(RESULT_CANCELED); finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MetricConfigScreen(
    widgetId: Int,
    profiles: List<AccountProfile>,
    existing: MetricWidgetConfig,
    loadCache: (profileId: String, phone: String) -> UsageCacheEntry?,
    onSave: (MetricWidgetConfig) -> Unit,
    onCancel: () -> Unit
) {
    var selectedProfileId by remember { mutableStateOf(existing.profileId ?: profiles.firstOrNull()?.id) }
    var selectedPhone     by remember { mutableStateOf(existing.telephoneNo) }
    var dataPoint         by remember { mutableStateOf(existing.dataPoint) }
    var themeMode         by remember { mutableStateOf(existing.themeMode) }
    var alpha             by remember { mutableFloatStateOf(existing.backgroundAlpha) }
    var arcColor          by remember { mutableStateOf(existing.color) }

    val selectedProfile = profiles.find { it.id == selectedProfileId }
    val phones = selectedProfile?.telephoneNumbers ?: emptyList()

    // Derive metric options from cache when a profile+phone is selected;
    // fall back to generic labels when no data has been fetched yet.
    val metricOptions: List<Pair<String, String>> = remember(selectedProfileId, selectedPhone) {
        val pid = selectedProfileId
        val ph  = selectedPhone ?: phones.firstOrNull()
        if (pid != null && ph != null) {
            val entry = loadCache(pid, ph)
            val summary = entry?.summary
            if (summary != null) {
                val items = buildWidgetItems(summary, entry.vasBundles)
                metricOptionsFromItems(items).ifEmpty { GENERIC_METRIC_OPTIONS }
            } else GENERIC_METRIC_OPTIONS
        } else GENERIC_METRIC_OPTIONS
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Configure Metric Widget") }) }) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Account ───────────────────────────────────────────────────────
            item { SectionHeader("Account") }
            item {
                if (profiles.isNotEmpty()) {
                    DropdownSetting(
                        label = "Account",
                        value = selectedProfile?.username ?: "Select",
                        options = profiles.map { it.username },
                        onSelect = { idx -> selectedProfileId = profiles[idx].id; selectedPhone = null }
                    )
                }
                if (phones.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    DropdownSetting(
                        label = "Phone number",
                        value = selectedPhone ?: phones.first(),
                        options = phones,
                        onSelect = { idx -> selectedPhone = phones[idx] }
                    )
                }
            }

            // ── Metric ────────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                SectionHeader("Metric to display")
                DropdownSetting(
                    label = "Metric",
                    value = metricOptions.find { it.first == dataPoint }?.second
                        ?: metricOptions.firstOrNull()?.second
                        ?: dataPoint,
                    options = metricOptions.map { it.second },
                    onSelect = { idx -> dataPoint = metricOptions[idx].first }
                )
            }

            // ── Appearance ────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                SectionHeader("Appearance")
                ThemeSelector(themeMode) { themeMode = it }
                Spacer(Modifier.height(12.dp))
                OpacitySelector(
                    value    = alpha,
                    onChange = { alpha = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ── Arc colour ────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                SectionHeader("Arc colour")
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "Colour",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    ColorPicker(
                        selected = arcColor,
                        onSelect = { arcColor = it }
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            // ── Buttons ───────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Button(
                        onClick = {
                            onSave(MetricWidgetConfig(
                                widgetId    = widgetId,
                                profileId   = selectedProfileId,
                                telephoneNo = selectedPhone ?: phones.firstOrNull(),
                                dataPoint   = dataPoint,
                                themeMode   = themeMode,
                                backgroundAlpha = alpha,
                                color       = arcColor
                            ))
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                    Spacer(Modifier.padding(horizontal = 8.dp))
                    androidx.compose.material3.OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
