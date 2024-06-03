package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.domain.ProductInfo
import java.util.Date

@Parcelize
data class EnrolledCourse(
    val auditAccessExpires: Date?,
    val created: String,
    val mode: String,
    val isActive: Boolean,
    val course: EnrolledCourseData,
    val certificate: Certificate?,
    val productInfo: ProductInfo?,
) : Parcelable {
    private val isAuditAccessExpired: Boolean
        get() = auditAccessExpires == null || Date().after(auditAccessExpires)

    private val isAuditMode: Boolean
        get() = EnrollmentMode.AUDIT.toString().equals(mode, ignoreCase = true)
    val isUpgradeable: Boolean
        get() = isAuditMode &&
                course.isStarted &&
                course.isUpgradeDeadlinePassed.not() &&
                productInfo != null
}
