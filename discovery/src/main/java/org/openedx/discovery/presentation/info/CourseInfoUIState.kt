package org.openedx.discovery.presentation.info

import java.util.concurrent.atomic.AtomicReference

sealed class CourseInfoUIState {
    data class CourseInfo(
        val initialUrl: String = "",
        val isPreLogin: Boolean = false,
        val enrollmentSuccess: AtomicReference<String> = AtomicReference("")
    ) : CourseInfoUIState()
}

enum class CourseInfoUIAction {
    WEB_PAGE_LOADED,
    WEB_PAGE_ERROR,
    RELOAD_WEB_PAGE
}
