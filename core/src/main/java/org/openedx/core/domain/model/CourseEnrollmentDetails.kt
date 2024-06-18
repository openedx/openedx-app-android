package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class CourseEnrollmentDetails(
    val id: String,
    val courseUpdates: String,
    val courseHandouts: String,
    val discussionUrl: String,
    val courseAccessDetails: CourseAccessDetails,
    val certificate: Certificate?,
    val enrollmentDetails: EnrollmentDetails,
    val courseInfoOverview: CourseInfoOverview,
) : Parcelable {
    fun isUpgradable(): Boolean {
        val start = courseInfoOverview.start ?: return false
        val upgradeDeadline = enrollmentDetails.upgradeDeadline ?: return false
        if (enrollmentDetails.mode != "audit") return false

        return start < Date() && getCourseMode() != null && upgradeDeadline > Date()
    }

    fun getCourseMode(): CourseMode? {
        return courseInfoOverview.courseModes
            .firstOrNull { it.slug == "verified" && !it.androidSku.isNullOrEmpty() }
    }
}
