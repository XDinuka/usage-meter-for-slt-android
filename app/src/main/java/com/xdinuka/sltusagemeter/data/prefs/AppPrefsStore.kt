package com.xdinuka.sltusagemeter.data.prefs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemePreference { SYSTEM, LIGHT, DARK }

@Singleton
class AppPrefsStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(
        ThemePreference.valueOf(
            prefs.getString(KEY_THEME, ThemePreference.SYSTEM.name) ?: ThemePreference.SYSTEM.name
        )
    )
    val themeFlow = _theme.asStateFlow()

    private val _refreshInterval = MutableStateFlow(prefs.getInt(KEY_REFRESH_INTERVAL, 30))
    val refreshIntervalFlow = _refreshInterval.asStateFlow()

    var refreshIntervalMinutes: Int
        get() = _refreshInterval.value
        set(value) {
            prefs.edit().putInt(KEY_REFRESH_INTERVAL, value).apply()
            _refreshInterval.value = value
        }

    fun setTheme(theme: ThemePreference) {
        prefs.edit().putString(KEY_THEME, theme.name).apply()
        _theme.value = theme
    }

    companion object {
        const val PREFS_NAME = "app_prefs"
        const val KEY_THEME = "theme_preference"
        const val KEY_REFRESH_INTERVAL = "widget_refresh_interval"
    }
}
