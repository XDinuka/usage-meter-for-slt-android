package com.xdinuka.sltusagemeter.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xdinuka.sltusagemeter.data.auth.ProfileStore
import com.xdinuka.sltusagemeter.ui.theme.SltTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SltWidgetConfigActivity : ComponentActivity() {

    @Inject lateinit var profileStore: ProfileStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        // Default to cancel so back press = no widget placed
        setResult(RESULT_CANCELED)

        setContent {
            SltTheme {
                WidgetConfigScreen(
                    widgetId = appWidgetId,
                    profiles = profileStore.profiles.value,
                    existing = WidgetConfigStore(this).getConfig(appWidgetId),
                    onSave = { config ->
                        WidgetConfigStore(this).saveConfig(config)
                        SltWidgetReceiver.enqueueOneTimeWork(this)
                        val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        setResult(RESULT_OK, result)
                        finish()
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigScreen(
    widgetId: Int,
    profiles: List<com.xdinuka.sltusagemeter.data.auth.AccountProfile>,
    existing: WidgetConfig,
    onSave: (WidgetConfig) -> Unit,
    onCancel: () -> Unit
) {
    var selectedProfileId by remember { mutableStateOf(existing.profileId ?: profiles.firstOrNull()?.id) }
    var selectedPhone by remember { mutableStateOf(existing.telephoneNo) }
    var themeMode by remember { mutableStateOf(existing.themeMode) }
    var alpha by remember { mutableFloatStateOf(existing.backgroundAlpha) }
    var showAccountDetails by remember { mutableStateOf(existing.showAccountDetails) }
    var showLastFetched by remember { mutableStateOf(existing.showLastFetched) }
    var enabledPoints by remember { mutableStateOf(existing.enabledDataPoints.toMutableSet()) }
    // Per-data-point colour map (starts from existing or defaults)
    var dataPointColors by remember {
        mutableStateOf(DEFAULT_DATA_POINT_COLORS + existing.dataPointColors)
    }

    val selectedProfile = profiles.find { it.id == selectedProfileId }
    val phones = selectedProfile?.telephoneNumbers ?: emptyList()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Configure Widget") }) }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item { SectionHeader("Account") }
            item {
                if (profiles.isNotEmpty()) {
                    DropdownSetting(
                        label = "Account",
                        value = selectedProfile?.username ?: "Select account",
                        options = profiles.map { it.username },
                        onSelect = { idx ->
                            selectedProfileId = profiles[idx].id
                            selectedPhone = null
                        }
                    )
                }
                if (phones.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    DropdownSetting(
                        label = "Phone number",
                        value = selectedPhone ?: phones.firstOrNull() ?: "—",
                        options = phones,
                        onSelect = { idx -> selectedPhone = phones[idx] }
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            item { SectionHeader("Appearance") }
            item {
                ThemeSelector(themeMode) { themeMode = it }
                Spacer(Modifier.height(8.dp))
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        "Background opacity: ${(alpha * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(value = alpha, onValueChange = { alpha = it }, valueRange = 0f..1f, steps = 19)
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Show subscriber ID",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = showAccountDetails, onCheckedChange = { showAccountDetails = it })
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Show last updated time",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = showLastFetched, onCheckedChange = { showLastFetched = it })
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            item { SectionHeader("Data points") }

            DataPointType.entries.forEach { dp ->
                item {
                    Column(Modifier.fillMaxWidth()) {
                        // Enable/disable row
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = dp.name in enabledPoints,
                                onCheckedChange = { checked ->
                                    enabledPoints = enabledPoints.toMutableSet().apply {
                                        if (checked) add(dp.name) else remove(dp.name)
                                    }
                                }
                            )
                            Text(dp.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                        // Colour picker row (always shown so user can pre-configure colours)
                        ColorPaletteRow(
                            selected = dataPointColors[dp.name] ?: DEFAULT_DATA_POINT_COLORS[dp.name] ?: "#2196F3",
                            onSelect = { hex ->
                                dataPointColors = dataPointColors.toMutableMap().apply { put(dp.name, hex) }
                            },
                            modifier = Modifier.padding(start = 52.dp, end = 16.dp, bottom = 8.dp)
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Button(
                        onClick = {
                            onSave(
                                WidgetConfig(
                                    widgetId = widgetId,
                                    profileId = selectedProfileId,
                                    telephoneNo = selectedPhone ?: phones.firstOrNull(),
                                    themeMode = themeMode,
                                    backgroundAlpha = alpha,
                                    showAccountDetails = showAccountDetails,
                                    enabledDataPoints = enabledPoints.toList(),
                                    dataPointColors = dataPointColors,
                                    showLastFetched = showLastFetched
                                )
                            )
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

// ── Shared colour picker ─────────────────────────────────────────────────────

/**
 * A horizontal row of coloured circles drawn from [WIDGET_COLOR_PALETTE].
 * The currently-selected swatch gets a white ring highlight.
 */
@Composable
internal fun ColorPaletteRow(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        WIDGET_COLOR_PALETTE.forEach { hex ->
            val parsed = parseHexColor(hex)
            val isSelected = hex.equals(selected, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(parsed)
                    .then(
                        if (isSelected) Modifier.border(2.dp, Color.White, CircleShape)
                        else Modifier
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}

/** Parses a hex string safely, falling back to a neutral grey. */
internal fun parseHexColor(hex: String): Color = try {
    val argb = android.graphics.Color.parseColor(hex)
    Color(argb.toLong() and 0xFFFFFFFFL)
} catch (_: Exception) {
    Color(0xFF9E9E9E)
}

// ── Reusable config UI helpers ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DropdownSetting(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEachIndexed { idx, opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = { onSelect(idx); expanded = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeSelector(current: String, onChange: (String) -> Unit) {
    val modes = WidgetThemeMode.entries
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("Theme", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            modes.forEachIndexed { idx, mode ->
                SegmentedButton(
                    selected = current == mode.name,
                    onClick = { onChange(mode.name) },
                    shape = SegmentedButtonDefaults.itemShape(idx, modes.size)
                ) { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
            }
        }
    }
}

@Composable
internal fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}
