package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class FirebaseConfig(
    @SerializedName("ENABLED")
    val enabled: Boolean = false,

    @SerializedName("ANALYTICS_SOURCE")
    val analyticsSource: AnalyticsSource = AnalyticsSource.NONE,

    @SerializedName("PROJECT_NUMBER")
    val projectNumber: String = "",

    @SerializedName("PROJECT_ID")
    val projectId: String = "",

    @SerializedName("APPLICATION_ID")
    val applicationId: String = "",

    @SerializedName("API_KEY")
    val apiKey: String = "",
) {
    fun isSegmentAnalyticsSource(): Boolean {
        return enabled && analyticsSource == AnalyticsSource.SEGMENT
    }
}
