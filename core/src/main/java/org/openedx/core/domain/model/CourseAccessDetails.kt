package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class CourseAccessDetails(
    val auditAccessExpires: Date?,
    val coursewareAccess: CoursewareAccess?,
) : Parcelable {
    val isAuditAccessExpired: Boolean
        get() = auditAccessExpires == null || Date().after(auditAccessExpires)
}
