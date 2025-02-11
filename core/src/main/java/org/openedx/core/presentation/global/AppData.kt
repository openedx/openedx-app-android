package org.openedx.core.presentation.global

data class AppData(
    val appName: String,
    val applicationId: String,
    val versionName: String,
) {
    val appUserAgent get() = "$appName/$applicationId/$versionName"
}
