package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import java.util.*

data class CourseDateBlock(
    @SerializedName("complete")
    val complete: Boolean = false,
    @SerializedName("date")
    val date: String = "",  // ISO 8601 compliant format
    @SerializedName("assignment_type")
    val assignmentType: String? = "",
    @SerializedName("date_type")
    val dateType: DateType = DateType.NONE,
    @SerializedName("description")
    val description: String = "",
    @SerializedName("learner_has_access")
    val learnerHasAccess: Boolean = false,
    @SerializedName("link")
    val link: String = "",
    @SerializedName("link_text")
    val linkText: String = "",
    @SerializedName("title")
    val title: String = "",
    // component blockId in-case of navigating inside the app for component available in mobile
    @SerializedName("first_component_block_id")
    val blockId: String = "",
)
