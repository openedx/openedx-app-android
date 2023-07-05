package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName

data class CourseList(
    @SerializedName("pagination")
    val pagination: Pagination,
    @SerializedName("results")
    val results: List<CourseDetails>?,
)