package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.discovery.EnrollmentDetailsDB
import org.openedx.core.utils.TimeUtils
import org.openedx.core.domain.model.EnrollmentDetails as DomainEnrollmentDetails

data class EnrollmentDetails(
    @SerializedName("created")
    var created: String?,
    @SerializedName("date")
    val date: String?,
    @SerializedName("mode")
    val mode: String?,
    @SerializedName("is_active")
    val isActive: Boolean = false,
    @SerializedName("upgrade_deadline")
    val upgradeDeadline: String?,
) {
    fun mapToDomain() = DomainEnrollmentDetails(
        created = TimeUtils.iso8601ToDate(date ?: ""),
        mode = mode,
        isActive = isActive,
        upgradeDeadline = TimeUtils.iso8601ToDate(upgradeDeadline ?: ""),
    )

    fun mapToRoomEntity() = EnrollmentDetailsDB(
        created = created,
        mode = mode,
        isActive = isActive,
        upgradeDeadline = upgradeDeadline,
    )
}
