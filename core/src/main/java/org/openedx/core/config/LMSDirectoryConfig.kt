package org.openedx.core.config

import com.google.gson.annotations.SerializedName

/**
 * Feature flag for the multi-tenant LMS Directory.
 *
 * When [enabled], the app can browse the Open edX platforms published by a site
 * registry ([directoryUrl]), re-theme to the one the learner picks, and sign in
 * against it. Off by default — the app then behaves as a stock single-tenant build.
 */
data class LMSDirectoryConfig(
    @SerializedName("ENABLED")
    val enabled: Boolean = false,

    @SerializedName("DIRECTORY_URL")
    val directoryUrl: String = "",

    @SerializedName("DIRECTORY_MODE")
    val directoryMode: String = "",
) {
    /** The feature only works with a registry to talk to. */
    val isReachable: Boolean get() = enabled && directoryUrl.isNotBlank()
}
