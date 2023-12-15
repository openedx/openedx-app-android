package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class DiscoveryConfig(
    @SerializedName("TYPE")
    private val viewType: String = Type.NATIVE.name,

    @SerializedName("WEBVIEW")
    val webViewConfig: DiscoveryWebViewConfig = DiscoveryWebViewConfig(),
) {
    enum class Type {
        NATIVE,
        WEBVIEW
    }

    fun isViewTypeWebView(): Boolean {
        return Type.WEBVIEW.name.equals(viewType, ignoreCase = true)
    }
}

data class DiscoveryWebViewConfig(
    @SerializedName("BASE_URL")
    val baseUrl: String = "",

    @SerializedName("COURSE_DETAIL_TEMPLATE")
    val courseUrlTemplate: String = "",

    @SerializedName("PROGRAM_DETAIL_TEMPLATE")
    val programUrlTemplate: String = "",
)
