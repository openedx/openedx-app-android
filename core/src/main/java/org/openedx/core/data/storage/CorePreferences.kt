package org.openedx.core.data.storage

import org.openedx.core.data.model.User
import org.openedx.core.domain.model.AppConfig
import org.openedx.core.domain.model.VideoSettings
import org.openedx.core.lmsdirectory.LmsHistoryEntry

interface CorePreferences {
    var accessToken: String
    var refreshToken: String
    var pushToken: String
    var accessTokenExpiresAt: Long
    var user: User?
    var videoSettings: VideoSettings
    var appConfig: AppConfig
    var canResetAppDirectory: Boolean
    var isRelativeDatesEnabled: Boolean

    /**
     * Base URL of the LMS the learner picked in the LMS Directory, or null when
     * none is selected (or the feature is off). When set, [org.openedx.core.config.Config.getApiHostURL]
     * returns this instead of the baked-in host.
     */
    var selectedBaseUrl: String?

    /** Accent color (hex, e.g. "#f15d49") of the selected LMS, used to re-theme the app. */
    var selectedLmsAccentColor: String?

    /** OAuth mobile client id of the selected LMS. Sign-in uses this instead of the config value. */
    var selectedOAuthClientId: String?

    /** Feedback email of the selected LMS. */
    var selectedFeedbackEmail: String?

    /** Logo URL of the selected LMS, shown on the sign-in screen. */
    var selectedLmsLogoUrl: String?

    /** Login background image URL of the selected LMS, shown behind the sign-in header. */
    var selectedLmsLoginBackgroundUrl: String?

    /** Human title of the selected LMS, shown in the sign-in "Change" banner. */
    var selectedLmsTitle: String?

    /**
     * True when the LMS registry runs in curated/institution mode. In that mode the
     * catalog is the org's own platforms, so there is no learner "Report this LMS".
     * Set when the directory config is fetched; read by the Profile tab to hide reports.
     */
    var lmsDirectoryCurated: Boolean

    /**
     * Recently selected LMS platforms, most-recent-first (capped). Shown as the
     * directory "History" section. Persists across logout (matches iOS): logging out
     * clears the pinned selection but keeps the history so the picker can offer it.
     */
    var lmsHistory: List<LmsHistoryEntry>

    suspend fun clearCorePreferences()
}
