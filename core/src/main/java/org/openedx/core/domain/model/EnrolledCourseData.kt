package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class EnrolledCourseData(
    val id: String,
    val name: String,
    val number: String,
    val org: String,
    val start: Date?,
    val startDisplay: String,
    val startType: String,
    val end: Date?,
    val dynamicUpgradeDeadline: String,
    val subscriptionId: String,
    val coursewareAccess: CoursewareAccess?,
    val media: Media?,
    val courseImage: String,
    val courseAbout: String,
    val courseSharingUtmParameters: CourseSharingUtmParameters,
    val courseUpdates: String,
    val courseHandouts: String,
    val discussionUrl: String,
    val videoOutline: String,
    val isSelfPaced: Boolean
) : Parcelable
