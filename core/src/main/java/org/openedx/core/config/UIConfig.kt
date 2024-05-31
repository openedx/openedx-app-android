package org.openedx.core.config

import com.google.gson.annotations.SerializedName

data class UIConfig(
    @SerializedName("COURSE_NESTED_LIST_ENABLED")
    val isCourseNestedListEnabled: Boolean = false,
    @SerializedName("COURSE_UNIT_PROGRESS_ENABLED")
    val isCourseUnitProgressEnabled: Boolean = false,
)
