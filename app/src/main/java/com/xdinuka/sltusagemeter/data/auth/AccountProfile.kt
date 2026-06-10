package com.xdinuka.sltusagemeter.data.auth

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class AccountProfile(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val displayName: String? = null,
    val accessToken: String,
    val refreshToken: String,
    /** Cached telephone numbers for this login — populated after first account fetch. */
    val telephoneNumbers: List<String> = emptyList()
)
