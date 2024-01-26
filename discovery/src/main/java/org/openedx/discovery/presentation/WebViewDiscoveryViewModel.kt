package org.openedx.discovery.presentation

import androidx.fragment.app.FragmentManager
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.utils.UrlUtils

class WebViewDiscoveryViewModel(
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val corePreferences: CorePreferences,
    private val router: DiscoveryRouter,
    private val querySearch: String,
) : BaseViewModel() {

    val uriScheme: String get() = config.getUriScheme()

    private val webViewConfig get() = config.getDiscoveryConfig().webViewConfig

    val isPreLogin get() = config.isPreLoginExperienceEnabled() && corePreferences.user == null

    private var _discoveryUrl = webViewConfig.baseUrl
    val discoveryUrl: String
        get() {
            return if (querySearch.isNotBlank()) {
                val queryParams: MutableMap<String, String> = HashMap()
                queryParams[UrlUtils.QUERY_PARAM_SEARCH] = querySearch
                UrlUtils.buildUrlWithQueryParams(_discoveryUrl, queryParams)
            } else {
                _discoveryUrl
            }
        }

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

    fun navigateToSignUp(fragmentManager: FragmentManager) {
        router.navigateToSignUp(fragmentManager, null)
    }

    fun navigateToSignIn(fragmentManager: FragmentManager) {
        router.navigateToSignIn(fragmentManager, null, null)
    }
}
