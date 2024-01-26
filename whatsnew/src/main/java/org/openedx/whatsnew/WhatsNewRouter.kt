package org.openedx.whatsnew

import androidx.fragment.app.FragmentManager

interface WhatsNewRouter {
    fun navigateToMain(fm: FragmentManager, courseId: String? = null, infoType: String? = null)
}
