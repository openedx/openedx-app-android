package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.discovery.EnrollmentDetailsDB
import org.openedx.core.utils.TimeUtils

import org.openedx.core.domain.model.EnrollmentDetails as DomainEnrollmentDetails

data class EnrollmentDetails(
    @SerializedName("created")
    var created: String?,

    @SerializedName("mode")
    var mode: String?,

    @SerializedName("is_active")
    var isActive: Boolean = false,

    @SerializedName("upgrade_deadline")
    var upgradeDeadline: String?,
) {
    fun mapToDomain(): DomainEnrollmentDetails {
        return DomainEnrollmentDetails(
            created = TimeUtils.iso8601ToDate(created ?: ""),
            mode = mode,
            isActive = isActive,
            upgradeDeadline = TimeUtils.iso8601ToDate(upgradeDeadline ?: ""),
        )
    }

    fun mapToRoomEntity() = EnrollmentDetailsDB(
        created = created,
        mode = mode,
        isActive = isActive,
        upgradeDeadline = upgradeDeadline,
    )
}
