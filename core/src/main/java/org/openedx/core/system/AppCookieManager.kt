package org.openedx.core.system

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.RequestBody
import org.openedx.core.config.Config
import org.openedx.core.data.api.CookiesApi
import retrofit2.Response
import java.util.concurrent.TimeUnit

class AppCookieManager(private val config: Config, private val api: CookiesApi) {

    companion object {
        private val FRESHNESS_INTERVAL = TimeUnit.HOURS.toMillis(1)
    }

    private var authSessionCookieExpiration: Long = -1
    private var response: Response<RequestBody>? = null

    suspend fun tryToRefreshSessionCookie() {
        try {
            response = api.userCookies()
            clearWebViewCookie()
            val cookieManager = CookieManager.getInstance()
            for (cookie in Cookie.parseAll(response!!.raw().request.url, response!!.headers())) {
                cookieManager.setCookie(config.getApiHostURL(), cookie.toString())
            }
            authSessionCookieExpiration = System.currentTimeMillis() + FRESHNESS_INTERVAL
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearWebViewCookie() {
        CookieManager.getInstance().removeAllCookies(null)
        authSessionCookieExpiration = -1
    }

    fun isSessionCookieMissingOrExpired(): Boolean {
        return authSessionCookieExpiration < System.currentTimeMillis()
    }
}
