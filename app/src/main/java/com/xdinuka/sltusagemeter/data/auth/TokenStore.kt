package com.xdinuka.sltusagemeter.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(
    @Named("tokenDataStore") dataStore: DataStore<Preferences>,
    aead: Aead,
) {
    private val store = EncryptedDataStore(dataStore, aead)

    // runBlocking is intentional: TokenAuthenticator runs on an OkHttp background thread
    // and already uses runBlocking to synchronise token refresh. Reading/writing a single
    // short string from DataStore is fast enough that blocking there is not a concern.

    var accessToken: String?
        get() = runBlocking { store.getString(KEY_ACCESS_TOKEN) }
        set(value) = runBlocking {
            if (value != null) store.putString(KEY_ACCESS_TOKEN, value)
            else store.remove(KEY_ACCESS_TOKEN)
        }

    var refreshToken: String?
        get() = runBlocking { store.getString(KEY_REFRESH_TOKEN) }
        set(value) = runBlocking {
            if (value != null) store.putString(KEY_REFRESH_TOKEN, value)
            else store.remove(KEY_REFRESH_TOKEN)
        }

    var username: String?
        get() = runBlocking { store.getString(KEY_USERNAME) }
        set(value) = runBlocking {
            if (value != null) store.putString(KEY_USERNAME, value)
            else store.remove(KEY_USERNAME)
        }

    fun isLoggedIn(): Boolean = accessToken != null

    fun clearAll() = runBlocking { store.clear() }

    companion object {
        private val KEY_ACCESS_TOKEN  = stringPreferencesKey("accessToken")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refreshToken")
        private val KEY_USERNAME      = stringPreferencesKey("username")
    }
}
