package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.utils.TimeUtils
import java.util.Date

@Parcelize
data class EnrollmentDetails(
    var created: Date?,
    var mode: String?,
    var isActive: Boolean,
    var upgradeDeadline: Date?,
) : Parcelable {
    val isUpgradeDeadlinePassed: Boolean
        get() = TimeUtils.isDatePassed(Date(), upgradeDeadline)

    val isAuditMode: Boolean
        get() = EnrollmentMode.AUDIT.toString().equals(mode, ignoreCase = true)
}
