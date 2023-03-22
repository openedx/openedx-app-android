package com.raccoongang.core.system

import android.webkit.CookieManager
import com.raccoongang.core.BuildConfig
import com.raccoongang.core.data.api.CookiesApi
import okhttp3.Cookie
import okhttp3.RequestBody
import retrofit2.Response
import java.util.concurrent.TimeUnit

class AppCookieManager(private val api: CookiesApi) {

    companion object{
        private const val REV_934_COOKIE =
            "REV_934=mobile; expires=Tue, 31 Dec 2021 12:00:20 GMT; domain=.edx.org;"
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
                cookieManager.setCookie(BuildConfig.BASE_URL,cookie.toString())
            }
            authSessionCookieExpiration = System.currentTimeMillis() + FRESHNESS_INTERVAL
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearWebViewCookie() {
        CookieManager.getInstance().removeAllCookies { result ->
            if (result) {
                authSessionCookieExpiration = -1
            }
        }
    }

    fun isSessionCookieMissingOrExpired(): Boolean {
        return authSessionCookieExpiration < System.currentTimeMillis()
    }

    fun setMobileCookie() {
        CookieManager.getInstance().setCookie(BuildConfig.BASE_URL, REV_934_COOKIE)
    }

}