package org.openedx.auth.presentation.sso

import android.app.Activity
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.browser.customtabs.CustomTabsIntent
import org.openedx.core.config.Config
import org.openedx.core.utils.Logger

class BrowserAuthHelper(private val config: Config) {

    private val logger = Logger(TAG)

    @WorkerThread
    suspend fun signIn(activityContext: Activity) {
        logger.d { "Browser-based auth initiated" }
        val uri = Uri.parse("${config.getApiHostURL()}/oauth2/authorize").buildUpon()
            .appendQueryParameter("client_id", config.getOAuthClientId())
            .appendQueryParameter("redirect_uri", "${activityContext.packageName}://oauth2Callback")
            .appendQueryParameter("response_type", "code").build()
        val intent =
            CustomTabsIntent.Builder().setUrlBarHidingEnabled(true).setShowTitle(true).build()
        intent.intent.setFlags(FLAG_ACTIVITY_NEW_TASK)
        logger.d { "Launching custom tab with ${uri.toString()}"}
        intent.launchUrl(activityContext, uri)
    }

    private companion object {
        const val TAG = "BrowserAuthHelper"
    }
}
