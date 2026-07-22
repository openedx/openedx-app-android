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
    /**
     * The single gate for activating any LMS Directory behaviour: the feature only
     * works with a registry to talk to, so an ENABLED:true build with a blank
     * [directoryUrl] stays fully single-tenant instead of building clients against an
     * invalid stub.
     *
     * This deliberately diverges from the white-label source, which gates purely on the
     * directory URL being non-blank. It is equivalent in effect (a directory URL is only
     * ever present when the feature is on) and safer, because it also refuses to activate
     * on the ENABLED:true + empty-URL misconfiguration.
     */
    val isReachable: Boolean get() = enabled && directoryUrl.isNotBlank()
}
