package com.xdinuka.sltusagemeter.data.auth

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.crypto.tink.Aead
import kotlinx.coroutines.flow.first

/**
 * Wraps a [DataStore] so every value is encrypted with [Tink AEAD][Aead] before writing
 * and decrypted on read. Values are Base64-encoded ciphertext strings on disk.
 *
 * TODO: Replace with androidx.security:security-datastore once it reaches stable.
 *       https://developer.android.com/jetpack/androidx/releases/security
 *       This class manually replicates what that library does internally.
 */
internal class EncryptedDataStore(
    private val dataStore: DataStore<Preferences>,
    private val aead: Aead,
) {
    // Empty associated data — sufficient for per-value encryption without binding context.
    private val ad = ByteArray(0)

    suspend fun getString(key: Preferences.Key<String>): String? {
        val encoded = dataStore.data.first()[key] ?: return null
        return runCatching {
            val ciphertext = Base64.decode(encoded, Base64.NO_WRAP)
            aead.decrypt(ciphertext, ad).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    suspend fun putString(key: Preferences.Key<String>, value: String) {
        val ciphertext = aead.encrypt(value.toByteArray(Charsets.UTF_8), ad)
        dataStore.edit { it[key] = Base64.encodeToString(ciphertext, Base64.NO_WRAP) }
    }

    suspend fun remove(vararg keys: Preferences.Key<String>) {
        dataStore.edit { prefs -> keys.forEach { prefs.remove(it) } }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}
