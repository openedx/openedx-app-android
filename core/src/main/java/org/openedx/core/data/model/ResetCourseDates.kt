package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.ResetCourseDates

data class ResetCourseDates(
    @SerializedName("message")
    val message: String = "",
    @SerializedName("body")
    val body: String = "",
    @SerializedName("header")
    val header: String = "",
    @SerializedName("link")
    val link: String = "",
    @SerializedName("link_text")
    val linkText: String = "",
) {
    fun mapToDomain(): ResetCourseDates {
        return ResetCourseDates(
            message = message,
            body = body,
            header = header,
            link = link,
            linkText = linkText,
        )
    }
}
