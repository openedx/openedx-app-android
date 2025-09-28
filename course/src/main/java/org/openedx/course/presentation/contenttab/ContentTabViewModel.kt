package org.openedx.course.presentation.contenttab

import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.course.presentation.container.CourseContentTab
import org.openedx.foundation.presentation.BaseViewModel

class ContentTabViewModel(
    val courseId: String,
    private val courseTitle: String,
    private val analytics: CourseAnalytics,
) : BaseViewModel() {

    fun logTabClickEvent(contentTab: CourseContentTab) {
        analytics.logEvent(
            CourseAnalyticsEvent.COURSE_CONTENT_TAB_CLICK.eventName,
            buildMap {
                put(
                    CourseAnalyticsKey.NAME.key,
                    CourseAnalyticsEvent.COURSE_CONTENT_TAB_CLICK.biValue
                )
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
                put(CourseAnalyticsKey.COURSE_NAME.key, courseTitle)
                put(CourseAnalyticsKey.TAB_NAME.key, contentTab.name)
            }
        )
    }
}
