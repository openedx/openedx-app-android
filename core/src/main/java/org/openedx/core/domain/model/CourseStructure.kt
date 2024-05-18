package org.openedx.core.domain.model

import org.openedx.core.domain.ProductInfo
import org.openedx.core.utils.TimeUtils
import java.util.Date

data class CourseStructure(
    val root: String,
    val blockData: List<Block>,
    val id: String,
    val name: String,
    val number: String,
    val org: String,
    val start: Date?,
    val startDisplay: String,
    val startType: String,
    val end: Date?,
    val coursewareAccess: CoursewareAccess?,
    val media: Media?,
    val courseAccessDetails: CourseAccessDetails,
    val certificate: Certificate?,
    val enrollmentDetails: EnrollmentDetails,
    val isSelfPaced: Boolean,
    val productInfo: ProductInfo?
) {
    private val isStarted: Boolean
        get() = TimeUtils.isDatePassed(Date(), start)
    val isUpgradeable: Boolean
        get() = enrollmentDetails.isAuditMode &&
                isStarted &&
                enrollmentDetails.isUpgradeDeadlinePassed.not() &&
                productInfo != null && courseAccessDetails.isAuditAccessExpired.not()
}
