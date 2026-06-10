package com.xdinuka.sltusagemeter.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.xdinuka.sltusagemeter.data.prefs.AppPrefsStore
import com.xdinuka.sltusagemeter.data.prefs.ThemePreference
import com.xdinuka.sltusagemeter.widget.SltWidgetReceiver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPrefsStore: AppPrefsStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val themePreference: StateFlow<ThemePreference> = appPrefsStore.themeFlow
    val refreshInterval: StateFlow<Int> = appPrefsStore.refreshIntervalFlow

    fun setTheme(theme: ThemePreference) {
        appPrefsStore.setTheme(theme)
    }

    fun setRefreshInterval(minutes: Int) {
        appPrefsStore.refreshIntervalMinutes = minutes
        // Re-enqueue periodic work with new interval
        SltWidgetReceiver.enqueuePeriodicWork(context)
    }
}
