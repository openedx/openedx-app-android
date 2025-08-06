package org.openedx.auth.presentation.sso

import android.app.Activity
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.browser.customtabs.CustomTabsIntent
import org.openedx.core.ApiConstants
import org.openedx.core.config.Config
import org.openedx.core.utils.Logger

class BrowserAuthHelper(private val config: Config) {

    private val logger = Logger(TAG)

    @WorkerThread
    suspend fun signIn(activityContext: Activity) {
        logger.d { "Browser-based auth initiated" }
        val uri = Uri.parse("${config.getApiHostURL()}${ApiConstants.URL_AUTHORIZE}").buildUpon()
            .appendQueryParameter("client_id", config.getOAuthClientId())
            .appendQueryParameter(
                "redirect_uri",
                "${activityContext.packageName}://${ApiConstants.BrowserLogin.REDIRECT_HOST}"
            )
            .appendQueryParameter("response_type", ApiConstants.BrowserLogin.RESPONSE_TYPE).build()
        val intent =
            CustomTabsIntent.Builder().setUrlBarHidingEnabled(true).setShowTitle(true).build()
        intent.intent.flags = FLAG_ACTIVITY_NEW_TASK
        intent.launchUrl(activityContext, uri)
    }

    private companion object {
        const val TAG = "BrowserAuthHelper"
    }
}
