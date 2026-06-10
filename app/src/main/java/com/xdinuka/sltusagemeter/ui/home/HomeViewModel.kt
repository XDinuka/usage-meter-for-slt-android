package com.xdinuka.sltusagemeter.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xdinuka.sltusagemeter.data.auth.ProfileStore
import com.xdinuka.sltusagemeter.data.prefs.UsageCacheStore
import com.xdinuka.sltusagemeter.data.repository.SltRepository
import com.xdinuka.sltusagemeter.ui.login.LoginUiState
import com.xdinuka.sltusagemeter.ui.usage.UsageUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountCardState(
    val profileId: String,
    val username: String,
    val displayName: String?,
    val telephoneNo: String,
    val usageState: UsageUiState,
    /** Epoch millis of the last successful fetch, or null if never fetched. */
    val lastFetchedAt: Long? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SltRepository,
    private val profileStore: ProfileStore,
    private val usageCacheStore: UsageCacheStore
) : ViewModel() {

    private val _cards = MutableStateFlow<List<AccountCardState>>(emptyList())
    val cards = _cards.asStateFlow()

    val hasNoProfiles = profileStore.profiles
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), profileStore.profiles.value.isEmpty())

    // ── Inline add-account login state ───────────────────────────────────────
    private val _loginState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val loginState = _loginState.asStateFlow()

    private val fetchJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            profileStore.profiles.collect { profiles ->
                // Rebuild card list; prefer existing in-memory state (keeps UI stable)
                val currentCards = _cards.value.associateBy { "${it.profileId}/${it.telephoneNo}" }

                _cards.value = profiles.flatMap { profile ->
                    if (profile.telephoneNumbers.isEmpty()) {
                        // Phone numbers not cached yet; keep existing placeholder or create one
                        listOf(
                            currentCards["${profile.id}/"]
                                ?: AccountCardState(
                                    profile.id, profile.username, profile.displayName,
                                    "", UsageUiState.Loading
                                )
                        )
                    } else {
                        profile.telephoneNumbers.map { phone ->
                            // Existing in-memory state takes priority (avoids flicker during refresh)
                            currentCards["${profile.id}/$phone"] ?: run {
                                val cached = usageCacheStore.load(profile.id, phone)
                                AccountCardState(
                                    profileId = profile.id,
                                    username = profile.username,
                                    displayName = profile.displayName,
                                    telephoneNo = phone,
                                    usageState = if (cached?.summary != null)
                                        UsageUiState.Success(cached.summary, cached.vasBundles)
                                    else UsageUiState.Loading,
                                    lastFetchedAt = cached?.lastFetchedAt
                                )
                            }
                        }
                    }
                }

                // Auto-fetch only when there is no cached data yet for a profile/phone
                profiles.forEach { profile ->
                    if (profile.telephoneNumbers.isEmpty()) {
                        // Need to fetch accounts first
                        val key = "profile_${profile.id}"
                        if (fetchJobs[key]?.isActive != true) fetchProfileCards(profile.id)
                    } else {
                        profile.telephoneNumbers.forEach { phone ->
                            if (usageCacheStore.load(profile.id, phone) == null) {
                                val key = "${profile.id}/$phone"
                                if (fetchJobs[key]?.isActive != true) {
                                    fetchJobs[key] = viewModelScope.launch { fetchUsage(profile.id, phone) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Fetch helpers ────────────────────────────────────────────────────────

    private fun fetchProfileCards(profileId: String) {
        val key = "profile_$profileId"
        fetchJobs[key]?.cancel()
        fetchJobs[key] = viewModelScope.launch {
            val profile = profileStore.getProfile(profileId) ?: return@launch
            try {
                val accounts = repository.fetchAccounts(profileId)
                if (accounts.isEmpty()) {
                    setCards(profileId) {
                        listOf(AccountCardState(
                            profile.id, profile.username, profile.displayName,
                            "", UsageUiState.Error("No accounts found")
                        ))
                    }
                    return@launch
                }
                // Show cached data for known phones immediately, Loading for new ones
                setCards(profileId) {
                    accounts.map { acc ->
                        val phone = acc.telephoneno ?: ""
                        val cached = if (phone.isNotBlank()) usageCacheStore.load(profileId, phone) else null
                        AccountCardState(
                            profileId = profile.id,
                            username = profile.username,
                            displayName = profile.displayName,
                            telephoneNo = phone,
                            usageState = if (cached?.summary != null)
                                UsageUiState.Success(cached.summary, cached.vasBundles)
                            else UsageUiState.Loading,
                            lastFetchedAt = cached?.lastFetchedAt
                        )
                    }
                }
                // Fetch usage only for phones with no cache
                accounts.forEach { acc ->
                    val phone = acc.telephoneno ?: ""
                    if (phone.isNotBlank() && usageCacheStore.load(profileId, phone) == null) {
                        launch { fetchUsage(profileId, phone) }
                    }
                }
            } catch (e: Exception) {
                setCards(profileId) {
                    listOf(AccountCardState(
                        profile.id, profile.username, profile.displayName,
                        "", UsageUiState.Error(e.message ?: "Failed to load")
                    ))
                }
            }
        }
    }

    private suspend fun fetchUsage(profileId: String, telephoneNo: String) {
        if (telephoneNo.isBlank()) return
        val hasCached = usageCacheStore.load(profileId, telephoneNo) != null
        // Only show spinner when there's nothing cached yet
        if (!hasCached) setCardState(profileId, telephoneNo, UsageUiState.Loading)
        try {
            val summary = repository.fetchUsageSummary(profileId, telephoneNo)
            val vas = repository.fetchVasBundles(profileId, telephoneNo)
            val now = System.currentTimeMillis()
            usageCacheStore.save(profileId, telephoneNo, summary, vas)
            setCardState(
                profileId, telephoneNo,
                if (summary != null) UsageUiState.Success(summary, vas)
                else UsageUiState.Error("No usage data")
            )
            setCardTimestamp(profileId, telephoneNo, now)
        } catch (e: Exception) {
            // If cached data exists, keep showing it — don't overwrite with error
            if (!hasCached) {
                setCardState(profileId, telephoneNo, UsageUiState.Error(e.message ?: "Failed"))
            }
        }
    }

    // ── Public actions ────────────────────────────────────────────────────────

    fun refresh(profileId: String, telephoneNo: String) {
        val key = "${profileId}/$telephoneNo"
        fetchJobs[key]?.cancel()
        fetchJobs[key] = viewModelScope.launch { fetchUsage(profileId, telephoneNo) }
    }

    fun refreshAll() {
        profileStore.profiles.value.forEach { profile ->
            if (profile.telephoneNumbers.isEmpty()) {
                fetchProfileCards(profile.id)
            } else {
                profile.telephoneNumbers.forEach { phone ->
                    refresh(profile.id, phone)
                }
            }
        }
    }

    fun removeProfile(profileId: String) {
        viewModelScope.launch { repository.logout(profileId) }
    }

    /** Inline add-account login. Resets state on success; profiles flow handles card creation. */
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _loginState.value = LoginUiState.Error("Please enter your email and password")
            return
        }
        viewModelScope.launch {
            _loginState.value = LoginUiState.Loading
            try {
                repository.login(username.trim(), password)
                _loginState.value = LoginUiState.Idle   // collapse the form
            } catch (e: Exception) {
                _loginState.value = LoginUiState.Error(e.message ?: "Login failed. Please try again.")
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginUiState.Idle
    }

    // ── State helpers ────────────────────────────────────────────────────────

    private fun setCards(profileId: String, block: () -> List<AccountCardState>) {
        _cards.update { current ->
            current.filter { it.profileId != profileId } + block()
        }
    }

    private fun setCardState(profileId: String, telephoneNo: String, state: UsageUiState) {
        _cards.update { cards ->
            cards.map { card ->
                if (card.profileId == profileId && card.telephoneNo == telephoneNo)
                    card.copy(usageState = state)
                else card
            }
        }
    }

    private fun setCardTimestamp(profileId: String, telephoneNo: String, timestamp: Long) {
        _cards.update { cards ->
            cards.map { card ->
                if (card.profileId == profileId && card.telephoneNo == telephoneNo)
                    card.copy(lastFetchedAt = timestamp)
                else card
            }
        }
    }
}
