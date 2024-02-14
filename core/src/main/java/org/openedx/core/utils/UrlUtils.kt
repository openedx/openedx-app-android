package org.openedx.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object UrlUtils {

    const val QUERY_PARAM_SEARCH = "q"

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

    /**
     * Utility function to remove the given query parameter from the URL
     * Ref: https://stackoverflow.com/a/56108097
     *
     * @param url        that needs to update
     * @param queryParam that needs to remove from the URL
     * @return The URL after removing the given params
     */
    private fun removeQueryParameterFromURL(url: String, queryParam: String): String {
        val uri = Uri.parse(url)
        val params = uri.queryParameterNames
        val newUri = uri.buildUpon().clearQuery()
        for (param in params) {
            if (queryParam != param) {
                newUri.appendQueryParameter(param, uri.getQueryParameter(param))
            }
        }
        return newUri.build().toString()
    }

    /**
     * Builds a valid URL with the given query params.
     *
     * @param url     The base URL.
     * @param queryParams The query params to add in the URL.
     * @return URL String with query params added to it.
     */
    fun buildUrlWithQueryParams(url: String, queryParams: Map<String, String>): String {
        val uriBuilder = Uri.parse(url).buildUpon()
        for ((key, value) in queryParams) {
            if (url.contains(key)) {
                removeQueryParameterFromURL(url, key)
            }
            uriBuilder.appendQueryParameter(key, value)
        }
        return uriBuilder.build().toString()
    }
}
