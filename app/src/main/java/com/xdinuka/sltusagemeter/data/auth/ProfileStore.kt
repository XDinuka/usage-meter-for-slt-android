package com.xdinuka.sltusagemeter.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileStore @Inject constructor(@ApplicationContext private val context: Context) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, AccountProfile::class.java)
    private val adapter = moshi.adapter<List<AccountProfile>>(listType)

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "slt_profiles_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _profiles = MutableStateFlow(loadFromPrefs())
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
            if (p.id == profileId) p.copy(accessToken = accessToken, refreshToken = refreshToken) else p
        })
    }

    fun updateProfile(profile: AccountProfile) {
        persist(_profiles.value.map { if (it.id == profile.id) profile else it })
    }

    fun getProfile(profileId: String) = _profiles.value.find { it.id == profileId }

    private fun loadFromPrefs(): List<AccountProfile> {
        val json = prefs.getString(KEY_PROFILES, null) ?: return emptyList()
        return runCatching { adapter.fromJson(json) }.getOrNull() ?: emptyList()
    }

    private fun persist(list: List<AccountProfile>) {
        prefs.edit().putString(KEY_PROFILES, adapter.toJson(list)).apply()
        _profiles.value = list
    }

    companion object {
        private const val KEY_PROFILES = "profiles"
    }
}
