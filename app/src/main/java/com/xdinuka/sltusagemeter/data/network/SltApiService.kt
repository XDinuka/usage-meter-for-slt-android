package com.xdinuka.sltusagemeter.data.network

import com.xdinuka.sltusagemeter.data.model.AccountResponse
import com.xdinuka.sltusagemeter.data.model.LoginResponse
import com.xdinuka.sltusagemeter.data.model.ServiceDetailResponse
import com.xdinuka.sltusagemeter.data.model.UsageSummaryResponse
import com.xdinuka.sltusagemeter.data.model.VasBundlesResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SltApiService {

    @FormUrlEncoded
    @POST("Account/Login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("channelID") channelId: String = "WEB"
    ): LoginResponse

    @FormUrlEncoded
    @POST("Account/RefreshToken")
    suspend fun refreshToken(
        @Field("username") username: String,
        @Field("refreshToken") refreshToken: String,
        @Field("channelID") channelId: String = "WEB"
    ): LoginResponse

    @GET("AccountOMNI/GetAccountDetailRequest")
    suspend fun getAccounts(@Query("username") username: String): AccountResponse

    @GET("AccountOMNI/GetServiceDetailRequest")
    suspend fun getServiceDetail(
        @Query("categoryID") categoryId: String = "BB",
        @Query("telephoneNo") telephoneNo: String
    ): ServiceDetailResponse

    @GET("BBVAS/UsageSummary")
    suspend fun getUsageSummary(@Query("subscriberID") subscriberID: String): UsageSummaryResponse

    @GET("BBVAS/GetDashboardVASBundles")
    suspend fun getVasBundles(@Query("subscriberID") subscriberID: String): VasBundlesResponse
}
