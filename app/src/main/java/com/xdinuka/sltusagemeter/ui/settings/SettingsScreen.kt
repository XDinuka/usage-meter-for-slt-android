package com.xdinuka.sltusagemeter.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xdinuka.sltusagemeter.data.prefs.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val themePreference by viewModel.themePreference.collectAsStateWithLifecycle()
    val refreshInterval by viewModel.refreshInterval.collectAsStateWithLifecycle()

    LazyColumn {
        item {
            Spacer(Modifier.height(8.dp))
            SectionHeader("Appearance")
        }

        item {
            SettingRow(
                title = "Theme",
                description = "Choose app colour scheme"
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = ThemePreference.entries
                    options.forEachIndexed { index, theme ->
                        SegmentedButton(
                            selected = themePreference == theme,
                            onClick = { viewModel.setTheme(theme) },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size)
                        ) {
                            Text(
                                text = theme.name.lowercase()
                                    .replaceFirstChar { it.uppercase() }
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            SectionHeader("Widget")
        }

        item {
            SettingRow(
                title = "Refresh interval",
                description = "How often widget data updates"
            ) {
                val intervals = listOf(15 to "15 min", 30 to "30 min", 60 to "1 hr")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    intervals.forEachIndexed { index, (minutes, label) ->
                        SegmentedButton(
                            selected = refreshInterval == minutes,
                            onClick = { viewModel.setRefreshInterval(minutes) },
                            shape = SegmentedButtonDefaults.itemShape(index, intervals.size)
                        ) { Text(label) }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    control: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        control()
    }
}
