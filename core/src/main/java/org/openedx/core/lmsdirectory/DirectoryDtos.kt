package org.openedx.core.lmsdirectory

import com.google.gson.annotations.SerializedName

/** Wire DTOs for the registry catalog. Mirrors the FastAPI response shapes. */

data class DirectoryListResponse(
    @SerializedName("items") val items: List<LmsSummaryDto> = emptyList(),
)

data class LmsSummaryDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("short_description") val shortDescription: String? = null,
    @SerializedName("base_url") val baseUrl: String,
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("accent_color") val accentColor: String? = null,
) {
    fun toDomain() = LmsSummary(
        id = id,
        title = title,
        shortDescription = shortDescription.orEmpty(),
        baseUrl = baseUrl,
        logoUrl = logoUrl,
        accentColor = accentColor,
    )
}

data class LmsDetailDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("base_url") val baseUrl: String,
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("accent_color") val accentColor: String? = null,
    @SerializedName("api") val api: ApiDto? = null,
) {
    data class ApiDto(
        @SerializedName("host_url") val hostUrl: String? = null,
        @SerializedName("oauth_client_id") val oauthClientId: String? = null,
        @SerializedName("feedback_email") val feedbackEmail: String? = null,
    )

    fun toDomain() = LmsDetail(
        id = id,
        title = title,
        baseUrl = api?.hostUrl?.ifBlank { null } ?: baseUrl,
        logoUrl = logoUrl,
        accentColor = accentColor,
        oauthClientId = api?.oauthClientId?.ifBlank { null },
        feedbackEmail = api?.feedbackEmail?.ifBlank { null },
    )
}

data class DirectoryConfigDto(
    @SerializedName("directory_mode") val directoryMode: String = "search",
    @SerializedName("provider_name") val providerName: String = "",
    @SerializedName("provider_tagline") val providerTagline: String = "",
) {
    fun toDomain() = DirectoryConfig(
        directoryMode = directoryMode,
        providerName = providerName,
        providerTagline = providerTagline,
    )
}

data class ReportRequestBody(
    @SerializedName("lms_id") val lmsId: Int?,
    @SerializedName("base_url") val baseUrl: String,
    @SerializedName("category") val category: String,
    @SerializedName("message") val message: String,
    @SerializedName("reporter_email") val reporterEmail: String?,
    @SerializedName("platform") val platform: String = "android",
    @SerializedName("app_version") val appVersion: String,
    @SerializedName("screenshot_base64") val screenshotBase64: String? = null,
)

data class ReportResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("status") val status: String,
)
