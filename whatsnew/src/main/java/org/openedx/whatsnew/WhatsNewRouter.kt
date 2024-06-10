package org.openedx.whatsnew

import androidx.fragment.app.FragmentManager

interface WhatsNewRouter {
    fun navigateToMain(
        fm: FragmentManager,
        courseId: String?,
        infoType: String?,
        openTab: String
    )
}
