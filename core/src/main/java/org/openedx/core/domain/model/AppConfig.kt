package org.openedx.core.domain.model

import java.io.Serializable

data class AppConfig(
    val courseDatesCalendarSync: CourseDatesCalendarSync,
) : Serializable

data class CourseDatesCalendarSync(
    val isEnabled: Boolean,
    val isSelfPacedEnabled: Boolean,
    val isInstructorPacedEnabled: Boolean,
    val isDeepLinkEnabled: Boolean,
) : Serializable
