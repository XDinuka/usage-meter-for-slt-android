package com.xdinuka.sltusagemeter.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color as AColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class SltWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val config = WidgetConfigStore(context).getConfig(widgetId)
        val state = WidgetStateStore.load(context, widgetId)

        val isDark = when (WidgetThemeMode.valueOf(config.themeMode)) {
            WidgetThemeMode.DARK -> true
            WidgetThemeMode.LIGHT -> false
            WidgetThemeMode.SYSTEM -> (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        val baseColor = if (isDark) AColor.parseColor("#FF1C1C1E") else AColor.WHITE
        val alpha = (config.backgroundAlpha.coerceIn(0f, 1f) * 255).toInt()
        val bgArgb = AColor.argb(alpha, AColor.red(baseColor), AColor.green(baseColor), AColor.blue(baseColor))
        val bgColor = Color(bgArgb.toLong() and 0xFFFFFFFFL)

        val textColor = if (isDark) Color.White else Color(0xFF212121)
        val subTextColor = if (isDark) Color(0xFFAAAAAA) else Color(0xFF757575)

        // Intent to open config — works both at initial placement and for reconfigure
        val configIntent = Intent(context, SltWidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(bgColor)
                        .padding(8.dp)
                ) {
                    WidgetContent(
                        state = state,
                        config = config,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        configIntent = configIntent
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetContent(
    state: WidgetDisplayState,
    config: WidgetConfig,
    textColor: Color,
    subTextColor: Color,
    configIntent: Intent
) {
    when (state) {
        is WidgetDisplayState.LoggedOut -> LoginPrompt(subTextColor)
        is WidgetDisplayState.Loading -> LoadingContent(subTextColor)
        is WidgetDisplayState.Success -> UsageContent(state, config, textColor, subTextColor, configIntent)
        is WidgetDisplayState.Error -> ErrorContent(state.message)
    }
}

@Composable
private fun LoginPrompt(subTextColor: Color) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Please Login", style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color(0xFF2196F3))))
        Spacer(GlanceModifier.height(4.dp))
        Text("Open the app to sign in", style = TextStyle(fontSize = 10.sp, color = ColorProvider(subTextColor)))
    }
}

@Composable
private fun LoadingContent(subTextColor: Color) {
    Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Loading...", style = TextStyle(fontSize = 12.sp, color = ColorProvider(subTextColor)))
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color(0xFFF44336))))
    }
}

@Composable
private fun UsageContent(
    state: WidgetDisplayState.Success,
    config: WidgetConfig,
    textColor: Color,
    subTextColor: Color,
    configIntent: Intent
) {
    val enabledTypes = config.enabledDataPoints.toSet()
    val visibleItems = state.items.filter { it.dataPointType in enabledTypes }

    Column(modifier = GlanceModifier.fillMaxSize()) {
        // ── Header row ──────────────────────────────────────────────────────
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (config.showAccountDetails) {
                Text(
                    text = state.subscriberID,
                    style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = ColorProvider(subTextColor)),
                    modifier = GlanceModifier.defaultWeight()
                )
            } else {
                Spacer(GlanceModifier.defaultWeight())
            }

            if (state.status.isNotBlank()) {
                StatusChip(state.status)
                Spacer(GlanceModifier.width(4.dp))
            }

            // ⚙ Reconfigure tap target
            Box(
                modifier = GlanceModifier
                    .size(20.dp)
                    .clickable(actionStartActivity(configIntent)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚙",
                    style = TextStyle(fontSize = 11.sp, color = ColorProvider(subTextColor.copy(alpha = 0.7f)))
                )
            }
        }

        Spacer(GlanceModifier.height(4.dp))

        // ── Progress rows ────────────────────────────────────────────────────
        visibleItems.take(6).forEach { item ->
            WidgetProgressRow(item = item, config = config, textColor = textColor, subTextColor = subTextColor)
            Spacer(GlanceModifier.height(4.dp))
        }

        // ── Last fetched timestamp ────────────────────────────────────────────
        if (config.showLastFetched && state.lastFetchedAt != null) {
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = widgetRelativeTime(state.lastFetchedAt),
                style = TextStyle(fontSize = 9.sp, color = ColorProvider(subTextColor.copy(alpha = 0.55f)))
            )
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val color = when (status.uppercase()) {
        "NORMAL", "ACTIVE" -> Color(0xFF4CAF50)
        "THROTTLED" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }
    Box(
        modifier = GlanceModifier
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(status, style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ColorProvider(color)))
    }
}

@Composable
private fun WidgetProgressRow(
    item: WidgetUsageItem,
    config: WidgetConfig,
    textColor: Color,
    subTextColor: Color
) {
    val color = parseWidgetColor(config.colorFor(item.dataPointType))
    val progress = (item.percentage / 100f).coerceIn(0f, 1f)
    val limitText = item.limit?.let { "${item.used} / $it ${item.volumeUnit}" } ?: "${item.used} ${item.volumeUnit}"

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = item.name,
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = ColorProvider(textColor)),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(limitText, style = TextStyle(fontSize = 9.sp, color = ColorProvider(subTextColor)))
        }
        Spacer(GlanceModifier.height(2.dp))
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier.fillMaxWidth().height(4.dp),
            color = ColorProvider(color),
            backgroundColor = ColorProvider(Color(0x1A000000))
        )
    }
}

/** Safely parses a hex colour string to a Compose [Color], defaulting to blue. */
private fun parseWidgetColor(hex: String): Color = try {
    Color(AColor.parseColor(hex).toLong() and 0xFFFFFFFFL)
} catch (_: Exception) {
    Color(0xFF2196F3)
}

/** Compact relative-time string suitable for the small widget footer. */
private fun widgetRelativeTime(epochMillis: Long): String {
    val diff = System.currentTimeMillis() - epochMillis
    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}
