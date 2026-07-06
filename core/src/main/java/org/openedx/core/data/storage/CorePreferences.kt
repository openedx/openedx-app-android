package org.openedx.core.data.storage

import org.openedx.core.data.model.User
import org.openedx.core.domain.model.AppConfig
import org.openedx.core.domain.model.VideoSettings

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

    suspend fun clearCorePreferences()
}
