package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.room.CourseInfoOverviewDb
import java.util.Date

@Parcelize
data class CourseInfoOverview(
    val name: String,
    val number: String,
    val org: String,
    val start: Date?,
    val startDisplay: String?,
    val startType: String,
    val end: Date?,
    val isSelfPaced: Boolean,
    var media: Media?,
    val courseSharingUtmParameters: CourseSharingUtmParameters,
    val courseAbout: String,
) : Parcelable {
    val isStarted: Boolean
        get() = start?.before(Date()) ?: false

    fun mapToEntity() = CourseInfoOverviewDb(
        name = name,
        number = number,
        org = org,
        start = start,
        startDisplay = startDisplay ?: "",
        startType = startType,
        end = end,
        isSelfPaced = isSelfPaced,
        media = media?.mapToEntity(),
        courseSharingUtmParameters = courseSharingUtmParameters.mapToEntity(),
        courseAbout = courseAbout
    )
}
