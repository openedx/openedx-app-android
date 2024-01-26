package org.openedx.course.presentation.info

import java.util.concurrent.atomic.AtomicReference

internal data class CourseInfoUIState(
    val initialUrl: String = "",
    val isPreLogin: Boolean = false,
    val enrollmentSuccess: AtomicReference<String> = AtomicReference("")
)
