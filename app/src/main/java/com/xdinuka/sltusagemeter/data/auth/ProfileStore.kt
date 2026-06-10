package com.xdinuka.sltusagemeter.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ProfileStore @Inject constructor(
    @Named("profileDataStore") dataStore: DataStore<Preferences>,
    aead: Aead,
) {
    private val moshi   = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter<List<AccountProfile>>(
        Types.newParameterizedType(List::class.java, AccountProfile::class.java)
    )

    private val store = EncryptedDataStore(dataStore, aead)

    // runBlocking on init: ProfileStore is injected lazily and the profile JSON is small
    // (<1 KB). Total blocking time is typically < 5 ms — well within ANR thresholds.
    private val _profiles = MutableStateFlow(loadFromStore())
    val profiles = _profiles.asStateFlow()

    fun isLoggedIn() = _profiles.value.isNotEmpty()

    fun addProfile(profile: AccountProfile) {
        persist(_profiles.value + profile)
    }

    fun removeProfile(profileId: String) {
        persist(_profiles.value.filter { it.id != profileId })
    }

    fun updateTokens(profileId: String, accessToken: String, refreshToken: String) {
        persist(_profiles.value.map { p ->
            if (p.id == profileId) p.copy(accessToken = accessToken, refreshToken = refreshToken)
            else p
        })
    }

    fun updateProfile(profile: AccountProfile) {
        persist(_profiles.value.map { if (it.id == profile.id) profile else it })
    }

    fun getProfile(profileId: String) = _profiles.value.find { it.id == profileId }

    private fun loadFromStore(): List<AccountProfile> {
        val json = runBlocking { store.getString(KEY_PROFILES) } ?: return emptyList()
        return runCatching { adapter.fromJson(json) }.getOrNull() ?: emptyList()
    }

    private fun persist(list: List<AccountProfile>) {
        _profiles.value = list
        runBlocking {
            if (list.isEmpty()) store.remove(KEY_PROFILES)
            else store.putString(KEY_PROFILES, adapter.toJson(list))
        }
    }

    companion object {
        private val KEY_PROFILES = stringPreferencesKey("profiles")
    }
}
