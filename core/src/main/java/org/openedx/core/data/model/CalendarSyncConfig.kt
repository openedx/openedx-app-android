package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.CourseDatesCalendarSync

data class CalendarSyncConfig(
    @SerializedName("android")
    val platformConfig: CalendarSyncPlatform = CalendarSyncPlatform(),
) {
    fun mapToDomain(): CourseDatesCalendarSync {
        return CourseDatesCalendarSync(
            isEnabled = platformConfig.enabled,
            isSelfPacedEnabled = platformConfig.selfPacedEnabled,
            isInstructorPacedEnabled = platformConfig.instructorPacedEnabled,
            isDeepLinkEnabled = platformConfig.deepLinksEnabled,
        )
    }
}

data class CalendarSyncPlatform(
    @SerializedName("enabled")
    val enabled: Boolean = false,
    @SerializedName("self_paced_enabled")
    val selfPacedEnabled: Boolean = false,
    @SerializedName("instructor_paced_enabled")
    val instructorPacedEnabled: Boolean = false,
    @SerializedName("deep_links_enabled")
    val deepLinksEnabled: Boolean = false,
)
