package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.EnrollmentDetails
import org.openedx.core.utils.TimeUtils

data class EnrollmentDetails(
    @SerializedName("date")
    val date: String?,
    @SerializedName("mode")
    val mode: String,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("upgrade_deadline")
    val upgradeDeadline: String?,
) {
    fun mapToDomain() = EnrollmentDetails(
        created = TimeUtils.iso8601ToDate(date ?: ""),
        mode = mode,
        isActive = isActive,
        upgradeDeadline = TimeUtils.iso8601ToDate(upgradeDeadline ?: ""),
    )
}
