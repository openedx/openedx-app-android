package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName

data class ShiftDueDatesBody(
    @SerializedName("course_keys") val courseKeys: List<String>
)