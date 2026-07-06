package org.openedx.core.lmsdirectory

import android.util.Log

/**
 * Talks to the LMS registry catalog. All calls return [Result] so callers can
 * fall back gracefully when the registry is unreachable.
 */
class LmsDirectoryRepository(
    private val api: LmsDirectoryApi,
    private val appVersion: String,
) {
    companion object {
        private const val TAG = "LmsDirectory"
    }

    suspend fun fetchConfig(): DirectoryConfig {
        return try {
            api.getConfig().toDomain()
        } catch (e: Exception) {
            Log.w(TAG, "Config fetch failed, defaulting to search mode: ${e.message}")
            DirectoryConfig.SEARCH_DEFAULT
        }
    }

    suspend fun search(query: String): Result<List<LmsSummary>> = runCatching {
        api.search(query = query.ifBlank { null }).items.map { it.toDomain() }
    }.onFailure { Log.w(TAG, "Search failed: ${it.message}") }

    suspend fun fetchFeatured(): Result<List<LmsSummary>> = runCatching {
        api.search(featured = true).items.map { it.toDomain() }
    }.onFailure { Log.w(TAG, "Featured fetch failed: ${it.message}") }

    /** Full record for one platform (includes the OAuth client id needed to sign in). */
    suspend fun fetchDetail(id: String): Result<LmsDetail> = runCatching {
        api.detail(id).toDomain()
    }.onFailure { Log.w(TAG, "Detail fetch failed: ${it.message}") }

    suspend fun submitReport(draft: ReportDraft): Result<Unit> = runCatching {
        api.submitReport(
            ReportRequestBody(
                lmsId = draft.lmsId?.toIntOrNull(),
                baseUrl = draft.baseUrl,
                category = draft.category.apiValue,
                message = draft.message,
                reporterEmail = draft.reporterEmail?.trim()?.ifBlank { null },
                appVersion = appVersion,
                screenshotBase64 = draft.screenshotBase64,
            )
        )
        Unit
    }.onFailure { Log.w(TAG, "Report submit failed: ${it.message}") }
}
