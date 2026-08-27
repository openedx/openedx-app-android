package org.openedx.app.deeplink

import android.content.Context
import com.appsflyer.AppsFlyerLib
import com.appsflyer.deeplink.DeepLinkListener
import com.appsflyer.deeplink.DeepLinkResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import org.openedx.core.utils.Logger

class AppsFlyerDeepLinkHandler {

    private val logger = Logger(TAG)

    private val _deepLink = MutableSharedFlow<DeepLink>(replay = 1, extraBufferCapacity = 1)
    val deepLink: SharedFlow<DeepLink> = _deepLink.asSharedFlow()

    fun init(context: Context, devKey: String) {
        val appsFlyerLib = AppsFlyerLib.getInstance()
        appsFlyerLib.subscribeForDeepLink(object : DeepLinkListener {
            override fun onDeepLinking(deepLinkResult: DeepLinkResult) {
                when (deepLinkResult.status) {
                    DeepLinkResult.Status.FOUND -> {
                        logger.i { "AppsFlyer deep link found." }
                        _deepLink.tryEmit(DeepLink(buildParams(deepLinkResult)))
                    }

                    DeepLinkResult.Status.NOT_FOUND -> {
                        logger.i { "AppsFlyer deep link not found." }
                    }

                    DeepLinkResult.Status.ERROR -> {
                        logger.e { "AppsFlyer deep link resolution failed. Caused by - ${deepLinkResult.error}" }
                    }
                }
            }
        })
        appsFlyerLib.init(devKey, null, context)
        appsFlyerLib.start(context)
    }

    // Defaults every OneLink click to the registration screen unless the OneLink
    // template explicitly sets a `screen_name` deep link parameter.
    private fun buildParams(deepLinkResult: DeepLinkResult): Map<String, String> {
        val clickEvent: JSONObject? = deepLinkResult.deepLink?.clickEvent
        val params = mutableMapOf<String, String>()
        clickEvent?.keys()?.forEach { key -> params[key] = clickEvent.optString(key) }

        if (!params.containsKey(DeepLink.Keys.SCREEN_NAME.value)) {
            params[DeepLink.Keys.SCREEN_NAME.value] = DeepLinkType.REGISTER.type
        }
        return params
    }

    companion object {
        private const val TAG = "AppsFlyer"
    }
}
