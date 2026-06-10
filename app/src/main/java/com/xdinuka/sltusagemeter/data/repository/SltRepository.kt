package com.xdinuka.sltusagemeter.data.repository

import com.xdinuka.sltusagemeter.data.auth.AccountProfile
import com.xdinuka.sltusagemeter.data.auth.ProfileStore
import com.xdinuka.sltusagemeter.data.model.AccountInfo
import com.xdinuka.sltusagemeter.data.model.ServiceDetailBundle
import com.xdinuka.sltusagemeter.data.model.UsageDetail
import com.xdinuka.sltusagemeter.data.model.UsageSummaryBundle
import com.xdinuka.sltusagemeter.data.network.ProfileApiClientFactory
import com.xdinuka.sltusagemeter.data.network.SltApiService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SltRepository @Inject constructor(
    val profileStore: ProfileStore,
    private val apiClientFactory: ProfileApiClientFactory,
    /** Login-only client (no bearer auth) injected from NetworkModule. */
    private val loginApiService: SltApiService
) {
    private val _logoutEvent = MutableSharedFlow<Unit>()
    val logoutEvent = _logoutEvent.asSharedFlow()

    val profiles: StateFlow<List<AccountProfile>> get() = profileStore.profiles

    fun isLoggedIn() = profileStore.isLoggedIn()

    /** Authenticate and store a new account profile. */
    suspend fun login(username: String, password: String) {
        val response = loginApiService.login(username, password)
        profileStore.addProfile(
            AccountProfile(
                username = username,
                displayName = response.name,
                accessToken = response.accessToken,
                refreshToken = response.refreshToken
            )
        )
    }

    /** Remove a profile and invalidate its API client. Emits [logoutEvent] if all profiles removed. */
    suspend fun logout(profileId: String) {
        profileStore.removeProfile(profileId)
        apiClientFactory.invalidate(profileId)
        if (!profileStore.isLoggedIn()) _logoutEvent.emit(Unit)
    }

    /** Fetch telephone numbers (accounts) for a profile. */
    suspend fun fetchAccounts(profileId: String): List<AccountInfo> {
        val profile = profileStore.getProfile(profileId) ?: error("Profile not found: $profileId")
        val accounts = apiClientFactory.getService(profile).getAccounts(profile.username).dataBundle ?: emptyList()
        // Cache telephone numbers on the profile so widget config activities can read them offline
        if (accounts.isNotEmpty()) {
            profileStore.updateProfile(
                profile.copy(telephoneNumbers = accounts.mapNotNull { it.telephoneno }.filter { it.isNotEmpty() })
            )
        }
        return accounts
    }

    suspend fun fetchServiceDetail(profileId: String, telephoneNo: String): ServiceDetailBundle? {
        val profile = profileStore.getProfile(profileId) ?: return null
        return apiClientFactory.getService(profile).getServiceDetail(telephoneNo = toIntl(telephoneNo)).dataBundle
    }

    suspend fun fetchUsageSummary(profileId: String, subscriberID: String): UsageSummaryBundle? {
        val profile = profileStore.getProfile(profileId) ?: return null
        return apiClientFactory.getService(profile).getUsageSummary(toIntl(subscriberID)).dataBundle
    }

    suspend fun fetchVasBundles(profileId: String, subscriberID: String): List<UsageDetail> {
        val profile = profileStore.getProfile(profileId) ?: return emptyList()
        return apiClientFactory.getService(profile).getVasBundles(toIntl(subscriberID)).dataBundle?.usageDetails ?: emptyList()
    }

    private fun toIntl(phone: String): String {
        val c = phone.replace(" ", "").replace("-", "")
        return when {
            c.startsWith("0") -> "94${c.drop(1)}"
            c.startsWith("94") -> c
            else -> "94$c"
        }
    }
}
