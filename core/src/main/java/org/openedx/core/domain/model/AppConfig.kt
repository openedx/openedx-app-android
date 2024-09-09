package org.openedx.core.domain.model

import java.io.Serializable

data class AppConfig(
    val courseDatesCalendarSync: CourseDatesCalendarSync = CourseDatesCalendarSync(),
) : Serializable

data class CourseDatesCalendarSync(
    val isEnabled: Boolean = false,
    val isSelfPacedEnabled: Boolean = false,
    val isInstructorPacedEnabled: Boolean = false,
    val isDeepLinkEnabled: Boolean = false,
) : Serializable
