package org.openedx.core.presentation.catalog

import android.net.Uri
import org.openedx.core.extension.getQueryParams

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
        COURSE("course")
    }

    object Param {
        const val PATH_ID = "path_id"
        const val COURSE_ID = "course_id"
        const val EMAIL_OPT = "email_opt_in"
        const val PROGRAMS = "programs"
    }

    companion object {
        fun parse(uriStr: String?, uriScheme: String): WebViewLink? {
            if (uriStr.isNullOrEmpty()) {
                return null
            }
            val sanitizedUriStr = uriStr.replace("+", "%2B")
            val uri = Uri.parse(sanitizedUriStr)

            // Validate the URI scheme
            if (uriScheme != uri.scheme) {
                return null
            }

            // Validate the Uri authority
            val uriAuthority = Authority.values().find { it.key == uri.authority } ?: return null

            // Parse the Uri params
            val params = uri.getQueryParams()

            return WebViewLink(uriAuthority, params)
        }
    }
}
