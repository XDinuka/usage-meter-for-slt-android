package com.xdinuka.sltusagemeter.data.network

import com.squareup.moshi.Moshi
import com.xdinuka.sltusagemeter.BuildConfig
import com.xdinuka.sltusagemeter.data.auth.AccountProfile
import com.xdinuka.sltusagemeter.data.auth.ProfileStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileApiClientFactory @Inject constructor(
    private val moshi: Moshi,
    private val profileStore: ProfileStore
) {
    private val services = ConcurrentHashMap<String, SltApiService>()

    fun getService(profile: AccountProfile): SltApiService =
        services.getOrPut(profile.id) { buildService(profile) }

    fun invalidate(profileId: String) {
        services.remove(profileId)
    }

    private fun buildService(profile: AccountProfile): SltApiService {
        val holder = TokenHolder(profile.accessToken, profile.refreshToken)
        var service: SltApiService? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(ProfileAuthInterceptor(holder))
            .authenticator(
                ProfileTokenAuthenticator(
                    profileId = profile.id,
                    username = profile.username,
                    holder = holder,
                    profileStore = profileStore,
                    apiService = { service!! }
                )
            )
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        service = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SltApiService::class.java)
        return service
    }

    companion object {
        const val BASE_URL = "https://omniscapp.slt.lk/slt/ext/api/"
    }
}

class TokenHolder(
    @Volatile var accessToken: String,
    @Volatile var refreshToken: String
)

class ProfileAuthInterceptor(private val holder: TokenHolder) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val builder = req.newBuilder().header("X-Ibm-Client-Id", BuildConfig.SLT_CLIENT_ID)
        val path = req.url.encodedPath
        if (!path.contains("Account/Login") && !path.contains("Account/RefreshToken")) {
            builder.header("authorization", "bearer ${holder.accessToken}")
        }
        return chain.proceed(builder.build())
    }
}

class ProfileTokenAuthenticator(
    private val profileId: String,
    private val username: String,
    private val holder: TokenHolder,
    private val profileStore: ProfileStore,
    private val apiService: () -> SltApiService
) : Authenticator {
    private val mutex = Mutex()
    private var lastRefreshTime = 0L

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("authorization-retry") != null) return null
        val newToken = runBlocking {
            mutex.withLock {
                if (System.currentTimeMillis() - lastRefreshTime < 5_000) return@withLock holder.accessToken
                try {
                    val result = apiService().refreshToken(username, holder.refreshToken)
                    holder.accessToken = result.accessToken
                    holder.refreshToken = result.refreshToken
                    profileStore.updateTokens(profileId, result.accessToken, result.refreshToken)
                    lastRefreshTime = System.currentTimeMillis()
                    result.accessToken
                } catch (e: Exception) {
                    profileStore.removeProfile(profileId)
                    null
                }
            }
        } ?: return null
        return response.request.newBuilder()
            .header("authorization", "bearer $newToken")
            .header("authorization-retry", "true")
            .build()
    }
}
