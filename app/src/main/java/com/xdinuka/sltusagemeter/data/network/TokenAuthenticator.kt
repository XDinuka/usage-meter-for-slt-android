package com.xdinuka.sltusagemeter.data.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Legacy single-account token authenticator.
 * Replaced by [ProfileTokenAuthenticator] — kept for reference only.
 */
class TokenAuthenticator : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? = null
}
