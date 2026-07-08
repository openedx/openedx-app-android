package org.openedx.core.lmsdirectory

/**
 * Domain models for the LMS registry catalog.
 * The registry is the same backend the iOS app talks to (`/api/v1/directory`).
 */

data class LmsSummary(
    val id: String,
    val title: String,
    val shortDescription: String,
    val baseUrl: String,
    val logoUrl: String?,
    val accentColor: String?,
)

/**
 * Full record for one platform, fetched when the learner picks it. Carries the
 * per-LMS OAuth client id and feedback email needed to actually sign in against it,
 * plus branding (logo, accent) — the catalog summary alone can't log you in.
 */
data class LmsDetail(
    val id: String,
    val title: String,
    val shortDescription: String = "",
    val baseUrl: String,
    val logoUrl: String?,
    val accentColor: String?,
    val oauthClientId: String?,
    val feedbackEmail: String?,
    val loginBackgroundUrl: String?,
    /** When true, the app opens the pre-login course Discovery screen instead of sign-in. */
    val preLoginDiscovery: Boolean = false,
)

/**
 * A platform the learner previously opened. Persisted (Gson JSON) so the directory
 * screen can show a "History" section when the search field is empty — mirrors iOS.
 * Carries the full detail needed to re-select without re-validating over the network.
 */
data class LmsHistoryEntry(
    val baseUrl: String,
    val title: String,
    val shortDescription: String = "",
    val logoUrl: String? = null,
    val accentColor: String? = null,
    val oauthClientId: String? = null,
    val feedbackEmail: String? = null,
    val loginBackgroundUrl: String? = null,
    val preLoginDiscovery: Boolean = false,
)

data class DirectoryConfig(
    val directoryMode: String,
    val providerName: String,
    val providerTagline: String,
) {
    val isCurated: Boolean get() = directoryMode.equals("curated", ignoreCase = true)

    companion object {
        val SEARCH_DEFAULT = DirectoryConfig(
            directoryMode = "search",
            providerName = "",
            providerTagline = "",
        )
    }
}

/**
 * Why a learner flags an LMS. Moderation reasons (trust & safety), not tech
 * support. Raw values match the registry's categories.
 */
enum class ReportCategory(val apiValue: String) {
    INAPPROPRIATE("inappropriate"),
    SCAM("scam"),
    IMPERSONATION("impersonation"),
    SPAM("spam"),
    BROKEN("broken"),
    OTHER("other"),
}

data class ReportDraft(
    val lmsId: String?,
    val baseUrl: String,
    val category: ReportCategory,
    val message: String,
    val reporterEmail: String?,
    /** A compressed screenshot as base64 (no data: prefix), or null. */
    val screenshotBase64: String? = null,
)
