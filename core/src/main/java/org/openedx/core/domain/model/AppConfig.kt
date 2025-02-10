package org.openedx.core.domain.model

import com.google.gson.annotations.SerializedName

data class AppConfig(
    val courseDatesCalendarSync: CourseDatesCalendarSync = CourseDatesCalendarSync(),
)

data class CourseDatesCalendarSync(
    @SerializedName("is_enabled")
    val isEnabled: Boolean = false,
    @SerializedName("is_self_paced_enabled")
    val isSelfPacedEnabled: Boolean = false,
    @SerializedName("is_instructor_paced_enabled")
    val isInstructorPacedEnabled: Boolean = false,
    @SerializedName("is_deep_link_enabled")
    val isDeepLinkEnabled: Boolean = false,
)
