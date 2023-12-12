package org.openedx.discovery.presentation

import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config

class WebViewDiscoveryViewModel(
    config: Config,
) : BaseViewModel() {

    private var _discoveryUrl = config.getDiscoveryConfig().webViewConfig.baseUrl
    val discoveryUrl: String
        get() = _discoveryUrl

    fun updateDiscoveryUrl(url: String) {
        if (url.isNotEmpty()) {
            _discoveryUrl = url
        }
    }
}
