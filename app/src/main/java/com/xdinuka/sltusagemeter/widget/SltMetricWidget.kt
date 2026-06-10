package com.xdinuka.sltusagemeter.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color as AColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class SltMetricWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val config = MetricWidgetConfigStore(context).getConfig(widgetId)
        val state = MetricWidgetStateStore.load(context, widgetId)

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

        // Parse the user-configured arc colour
        val progressColor = try {
            AColor.parseColor(config.color)
        } catch (_: Exception) {
            AColor.parseColor("#FF2196F3")
        }

        val trackColor = if (isDark)
            AColor.argb(40, 255, 255, 255)
        else
            AColor.argb(40, 0, 0, 0)

        // Intent to open config for reconfiguration
        val configIntent = Intent(context, MetricWidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(bgColor)
                        .padding(4.dp)
                        .clickable(actionStartActivity(configIntent)),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        is MetricWidgetState.Success -> {
                            val bitmap = CircularProgressBitmap.create(
                                sizePx = 200,
                                progress = state.percentage / 100f,
                                progressColor = progressColor,
                                trackColor = trackColor,
                                bgColor = bgArgb,
                                isDark = isDark,
                                labelText = state.name,
                                valueText = "${state.percentage}%"
                            )
                            Image(
                                provider = ImageProvider(bitmap),
                                contentDescription = "${state.name}: ${state.percentage}%",
                                modifier = GlanceModifier.fillMaxSize()
                            )
                        }

                        is MetricWidgetState.Loading -> {
                            Text(
                                "...",
                                style = TextStyle(fontSize = 14.sp, color = ColorProvider(Color(0xFF757575)))
                            )
                        }

                        is MetricWidgetState.LoggedOut -> {
                            Text(
                                "Login",
                                style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(0xFF2196F3)))
                            )
                        }

                        is MetricWidgetState.Error -> {
                            Text(
                                "!",
                                style = TextStyle(fontSize = 18.sp, color = ColorProvider(Color(0xFFF44336)))
                            )
                        }
                    }
                }
            }
        }
    }
}
