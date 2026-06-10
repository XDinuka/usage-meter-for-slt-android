package com.xdinuka.sltusagemeter.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xdinuka.sltusagemeter.data.model.UsageDetail
import com.xdinuka.sltusagemeter.data.model.UsageSummaryBundle
import com.xdinuka.sltusagemeter.data.repository.SltRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UsageUiState {
    data object Loading : UsageUiState()
    data class Success(
        val summary: UsageSummaryBundle,
        val vasBundles: List<UsageDetail>
    ) : UsageUiState()
    data class Error(val message: String) : UsageUiState()
}

@HiltViewModel
class UsageViewModel @Inject constructor(
    private val repository: SltRepository
) : ViewModel() {

    private val _state = MutableStateFlow<UsageUiState>(UsageUiState.Loading)
    val state = _state.asStateFlow()

    fun loadUsage(profileId: String, subscriberID: String) {
        viewModelScope.launch {
            _state.value = UsageUiState.Loading
            try {
                val summaryDeferred = async { repository.fetchUsageSummary(profileId, subscriberID) }
                val vasDeferred = async { repository.fetchVasBundles(profileId, subscriberID) }
                val summary = summaryDeferred.await()
                val vas = vasDeferred.await()
                _state.value = if (summary != null) UsageUiState.Success(summary, vas)
                else UsageUiState.Error("No usage data available")
            } catch (e: Exception) {
                _state.value = UsageUiState.Error(e.message ?: "Failed to load usage data")
            }
        }
    }
}
