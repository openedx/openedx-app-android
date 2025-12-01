package org.openedx.discovery

import org.openedx.core.domain.model.Media
import org.openedx.discovery.domain.model.Course

object DiscoveryMocks {
    val course = Course(
        id = "id",
        blocksUrl = "blocksUrl",
        courseId = "courseId",
        effort = "effort",
        enrollmentStart = null,
        enrollmentEnd = null,
        hidden = false,
        invitationOnly = false,
        media = Media(),
        mobileAvailable = true,
        name = "Test course",
        number = "number",
        org = "EdX",
        pacing = "pacing",
        shortDescription = "shortDescription",
        start = "start",
        end = "end",
        startDisplay = "startDisplay",
        startType = "startType",
        overview = "",
        isEnrolled = false
    )

    fun courses(count: Int) = List(count) { course }
}
