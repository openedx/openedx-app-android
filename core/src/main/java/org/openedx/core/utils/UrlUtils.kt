package org.openedx.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object UrlUtils {
    fun openInBrowser(activity: Context, apiHostUrl: String, url: String) {
        if (url.isEmpty()) {
            return
        }
        if (url.startsWith("/")) {
            // Use API host as the base URL for relative paths
            val absoluteUrl = "$apiHostUrl$url"
            openInBrowser(activity, absoluteUrl)
            return
        }
        openInBrowser(activity, url)
    }

    private fun openInBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setData(Uri.parse(url))
        context.startActivity(intent)
    }
}
