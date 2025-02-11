package org.openedx.discovery.presentation.catalog

import android.net.Uri
import org.openedx.foundation.extension.getQueryParams

/**
 * To parse and store links that we need within a WebView.
 */
class WebViewLink(
    var authority: Authority,
    var params: Map<String, String>
) {
    enum class Authority(val key: String) {
        COURSE_INFO("course_info"),
        PROGRAM_INFO("program_info"),
        ENROLL("enroll"),
        ENROLLED_PROGRAM_INFO("enrolled_program_info"),
        ENROLLED_COURSE_INFO("enrolled_course_info"),
        COURSE("course"),
        EXTERNAL("external"),
    }

    object Param {
        const val PATH_ID = "path_id"
        const val COURSE_ID = "course_id"
        const val EMAIL_OPT = "email_opt_in"
        const val PROGRAMS = "programs"
    }

    companion object {
        fun parse(uriStr: String?, uriScheme: String): WebViewLink? {
            if (uriStr.isNullOrEmpty()) return null

            val sanitizedUriStr = uriStr.replace("+", "%2B")
            val uri = Uri.parse(sanitizedUriStr)

            // Validate URI scheme and authority
            val isSchemeValid = uriScheme == uri.scheme
            val uriAuthority = Authority.entries.find { it.key == uri.authority }

            return if (isSchemeValid && uriAuthority != null) {
                // Parse the URI params
                val params = uri.getQueryParams()
                WebViewLink(uriAuthority, params)
            } else {
                null
            }
        }
    }
}
