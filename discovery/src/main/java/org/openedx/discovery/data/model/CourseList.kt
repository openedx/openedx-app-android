package org.openedx.discovery.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.Pagination

data class CourseList(
    @SerializedName("pagination")
    val pagination: Pagination,
    @SerializedName("results")
    val results: List<CourseDetails>?,
)
