package com.xdinuka.sltusagemeter.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "accessToken") val accessTokenCamel: String? = null,
    @Json(name = "access_token") val accessTokenSnake: String? = null,
    @Json(name = "refreshToken") val refreshTokenCamel: String? = null,
    @Json(name = "refresh_token") val refreshTokenSnake: String? = null,
    val name: String? = null,
    @Json(name = "userId") val userId: String? = null
) {
    val accessToken: String get() = accessTokenCamel ?: accessTokenSnake ?: ""
    val refreshToken: String get() = refreshTokenCamel ?: refreshTokenSnake ?: ""
}

@JsonClass(generateAdapter = true)
data class AccountResponse(
    @Json(name = "isSuccess") val isSuccess: Boolean = false,
    @Json(name = "dataBundle") val dataBundle: List<AccountInfo>? = null
)

@JsonClass(generateAdapter = true)
data class AccountInfo(
    @Json(name = "accountno") val accountno: String? = "",
    @Json(name = "telephoneno") val telephoneno: String? = "",
    @Json(name = "status") val status: String? = ""
)

@JsonClass(generateAdapter = true)
data class ServiceDetailResponse(
    @Json(name = "isSuccess") val isSuccess: Boolean = false,
    @Json(name = "dataBundle") val dataBundle: ServiceDetailBundle? = null
)

@JsonClass(generateAdapter = true)
data class ServiceDetailBundle(
    @Json(name = "accountNo") val accountNo: String? = "",
    @Json(name = "promotionName") val promotionName: String? = null,
    @Json(name = "contactNamewithInit") val contactNamewithInit: String? = null,
    @Json(name = "listofBBService") val listofBBService: List<BBService>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class BBService(
    @Json(name = "serviceID") val serviceID: String? = "",
    @Json(name = "packageName") val packageName: String? = "",
    @Json(name = "serviceStatus") val serviceStatus: String? = "",
    @Json(name = "serviceType") val serviceType: String? = ""
)

@JsonClass(generateAdapter = true)
data class UsageSummaryResponse(
    @Json(name = "isSuccess") val isSuccess: Boolean = false,
    @Json(name = "dataBundle") val dataBundle: UsageSummaryBundle? = null
)

@JsonClass(generateAdapter = true)
data class UsageSummaryBundle(
    @Json(name = "status") val status: String? = "",
    @Json(name = "my_package_info") val myPackageInfo: PackageInfo? = null,
    @Json(name = "my_package_summary") val myPackageSummary: PackageSummary? = null,
    @Json(name = "bonus_data_summary") val bonusDataSummary: PackageSummary? = null,
    @Json(name = "extra_gb_data_summary") val extraGbDataSummary: PackageSummary? = null
)

@JsonClass(generateAdapter = true)
data class PackageSummary(
    @Json(name = "limit") val limit: String? = "0",
    @Json(name = "used") val used: String? = "0",
    @Json(name = "volume_unit") val volumeUnit: String? = "GB"
)

@JsonClass(generateAdapter = true)
data class PackageInfo(
    @Json(name = "package_name") val packageName: String? = "",
    @Json(name = "usageDetails") val usageDetails: List<UsageDetail>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class UsageDetail(
    @Json(name = "name") val name: String = "",
    @Json(name = "limit") val limit: String? = null,
    @Json(name = "remaining") val remaining: String? = null,
    @Json(name = "used") val used: String? = "0",
    @Json(name = "percentage") val percentage: Int = 0,
    @Json(name = "volume_unit") val volumeUnit: String? = "GB",
    @Json(name = "expiry_date") val expiryDate: String? = null,
    @Json(name = "subscriptionid") val subscriptionId: String? = null,
    @Json(name = "timestamp") val timestamp: Long = 0,
    @Json(name = "unsubscribable") val unsubscribable: Boolean = false,
    @Json(name = "claim") val claim: String? = null
)

@JsonClass(generateAdapter = true)
data class VasBundlesResponse(
    @Json(name = "isSuccess") val isSuccess: Boolean = false,
    @Json(name = "dataBundle") val dataBundle: VasBundlesData? = null
)

@JsonClass(generateAdapter = true)
data class VasBundlesData(
    @Json(name = "usageDetails") val usageDetails: List<UsageDetail> = emptyList()
)
