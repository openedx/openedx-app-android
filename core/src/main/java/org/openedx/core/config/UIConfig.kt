package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class UIConfig(
    @SerializedName("COURSE_DROPDOWN_NAVIGATION_ENABLED")
    val isCourseDropdownNavigationEnabled: Boolean = false,
    @SerializedName("COURSE_UNIT_PROGRESS_ENABLED")
    val isCourseUnitProgressEnabled: Boolean = false,
    @SerializedName("COURSE_DOWNLOAD_QUEUE_SCREEN")
    val isCourseDownloadQueueEnabled: Boolean = false,
)
