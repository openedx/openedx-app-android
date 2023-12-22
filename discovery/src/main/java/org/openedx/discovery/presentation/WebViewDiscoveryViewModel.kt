package org.openedx.discovery.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.core.system.connection.NetworkConnection

class WebViewDiscoveryViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val router: DiscoveryRouter,
) : BaseViewModel() {

    val webViewConfig get() = config.getDiscoveryConfig().webViewConfig

    private var _discoveryUrl = webViewConfig.baseUrl
    val discoveryUrl: String
        get() = _discoveryUrl

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    fun updateDiscoveryUrl(url: String) {
        if (url.isNotEmpty()) {
            _discoveryUrl = url
        }
    }

    fun infoCardClicked(fragmentManager: FragmentManager, pathId: String, infoType: String) {
        if (pathId.isNotEmpty() && infoType.isNotEmpty()) {
            router.navigateToCourseInfo(
                fragmentManager,
                pathId,
                infoType
            )
        }
    }
}
