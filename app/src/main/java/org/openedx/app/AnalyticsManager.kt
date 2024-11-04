package org.openedx.app

import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.core.presentation.CoreAnalytics
import org.openedx.core.presentation.dialog.appreview.AppReviewAnalytics
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.dashboard.presentation.DashboardAnalytics
import org.openedx.discovery.presentation.DiscoveryAnalytics
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.foundation.interfaces.Analytics
import org.openedx.profile.presentation.ProfileAnalytics
import org.openedx.whatsnew.presentation.WhatsNewAnalytics

class AnalyticsManager :
    AppAnalytics,
    AppReviewAnalytics,
    AuthAnalytics,
    CoreAnalytics,
    CourseAnalytics,
    DashboardAnalytics,
    DiscoveryAnalytics,
    DiscussionAnalytics,
    ProfileAnalytics,
    WhatsNewAnalytics {

    private val analytics: MutableList<Analytics> = mutableListOf()

    fun addAnalyticsTracker(analytic: Analytics) {
        analytics.add(analytic)
    }

    private fun logEvent(event: Event, params: Map<String, Any?> = mapOf()) {
        analytics.forEach { analytics ->
            analytics.logEvent(event.eventName, params)
        }
    }

    override fun logScreenEvent(screenName: String, params: Map<String, Any?>) {
        analytics.forEach { analytics ->
            analytics.logScreenEvent(screenName, params)
        }
    }

    override fun logEvent(event: String, params: Map<String, Any?>) {
        analytics.forEach { analytics ->
            analytics.logEvent(event, params)
        }
    }

    private fun setUserId(userId: Long) {
        analytics.forEach { analytics ->
            analytics.logUserId(userId)
        }
    }

    override fun dashboardCourseClickedEvent(
        courseId: String,
        courseName: String
    ) {
        logEvent(
            Event.DASHBOARD_COURSE_CLICKED,
            buildMap {
                put(Key.COURSE_ID.keyName, courseId)
                put(Key.COURSE_NAME.keyName, courseName)
            }
        )
    }

    override fun logoutEvent(force: Boolean) {
        logEvent(
            Event.USER_LOGOUT,
            buildMap {
                put(Key.FORCE.keyName, force)
            }
        )
    }

    override fun setUserIdForSession(userId: Long) {
        setUserId(userId)
    }

    override fun discoverySearchBarClickedEvent() {
        logEvent(Event.DISCOVERY_SEARCH_BAR_CLICKED)
    }

    override fun discoveryCourseSearchEvent(label: String, coursesCount: Int) {
        logEvent(
            Event.DISCOVERY_COURSE_SEARCH,
            buildMap {
                put(Key.LABEL.keyName, label)
                put(Key.COURSE_COUNT.keyName, coursesCount)
            }
        )
    }

    override fun discoveryCourseClickedEvent(courseId: String, courseName: String) {
        logEvent(
            Event.DISCOVERY_COURSE_CLICKED,
            buildMap {
                put(Key.COURSE_ID.keyName, courseId)
                put(Key.COURSE_NAME.keyName, courseName)
            }
        )
    }

    override fun sequentialClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String,
    ) {
        logEvent(
            Event.SEQUENTIAL_CLICKED,
            buildMap {
                put(Key.COURSE_ID.keyName, courseId)
                put(Key.COURSE_NAME.keyName, courseName)
                put(Key.BLOCK_ID.keyName, blockId)
                put(Key.BLOCK_NAME.keyName, blockName)
            }
        )
    }

    override fun nextBlockClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String,
    ) {
        logEvent(
            Event.NEXT_BLOCK_CLICKED,
            buildMap {
                put(Key.COURSE_ID.keyName, courseId)
                put(Key.COURSE_NAME.keyName, courseName)
                put(Key.BLOCK_ID.keyName, blockId)
                put(Key.BLOCK_NAME.keyName, blockName)
            }
        )
    }

    override fun prevBlockClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String,
    ) {
        logEvent(
            Event.PREV_BLOCK_CLICKED,
            buildMap {
                put(Key.COURSE_ID.keyName, courseId)
                put(Key.COURSE_NAME.keyName, courseName)
                put(Key.BLOCK_ID.keyName, blockId)
                put(Key.BLOCK_NAME.keyName, blockName)
            }
        )
    }

    override fun finishVerticalClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String,
    ) {
        logEvent(
            Event.FINISH_VERTICAL_CLICKED,
            buildMap {
                put(Key.COURSE_ID.keyName, courseId)
                put(Key.COURSE_NAME.keyName, courseName)
                put(Key.BLOCK_ID.keyName, blockId)
                put(Key.BLOCK_NAME.keyName, blockName)
            }
        )
    }

    override fun finishVerticalNextClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String,
    ) {
        logEvent(
            Event.FINISH_VERTICAL_NEXT_CLICKED,
            buildMap {
                put(Key.COURSE_ID.keyName, courseId)
                put(Key.COURSE_NAME.keyName, courseName)
                put(Key.BLOCK_ID.keyName, blockId)
                put(Key.BLOCK_NAME.keyName, blockName)
            }
        )
    }

    override fun finishVerticalBackClickedEvent(
        courseId: String,
        courseName: String
    ) {
        logEvent(
            Event.FINISH_VERTICAL_BACK_CLICKED,
            buildMap {
                put(Key.COURSE_ID.keyName, courseId)
                put(Key.COURSE_NAME.keyName, courseName)
            }
        )
    }

    override fun discussionAllPostsClickedEvent(
        courseId: String,
        courseName: String
    ) {
        logEvent(
            Event.DISCUSSION_ALL_POSTS_CLICKED,
            buildMap {
                put(Key.COURSE_ID.keyName, courseId)
                put(Key.COURSE_NAME.keyName, courseName)
            }
        )
    }

    override fun discussionFollowingClickedEvent(
        courseId: String,
        courseName: String
    ) {
        logEvent(
            Event.DISCUSSION_FOLLOWING_CLICKED,
            buildMap {
                put(Key.COURSE_ID.keyName, courseId)
                put(Key.COURSE_NAME.keyName, courseName)
            }
        )
    }

    override fun discussionTopicClickedEvent(
        courseId: String,
        courseName: String,
        topicId: String,
        topicName: String,
    ) {
        logEvent(
            Event.DISCUSSION_TOPIC_CLICKED,
            buildMap {
                put(Key.COURSE_ID.keyName, courseId)
                put(Key.COURSE_NAME.keyName, courseName)
                put(Key.TOPIC_ID.keyName, topicId)
                put(Key.TOPIC_NAME.keyName, topicName)
            }
        )
    }
}

enum class Event(val eventName: String) {
    USER_LOGOUT("User_Logout"),
    DISCOVERY_SEARCH_BAR_CLICKED("Discovery_Search_Bar_Clicked"),
    DISCOVERY_COURSE_SEARCH("Discovery_Courses_Search"),
    DISCOVERY_COURSE_CLICKED("Discovery_Course_Clicked"),
    DASHBOARD_COURSE_CLICKED("Dashboard_Course_Clicked"),

    SEQUENTIAL_CLICKED("Sequential_Clicked"),
    NEXT_BLOCK_CLICKED("Next_Block_Clicked"),
    PREV_BLOCK_CLICKED("Prev_Block_Clicked"),
    FINISH_VERTICAL_CLICKED("Finish_Vertical_Clicked"),
    FINISH_VERTICAL_NEXT_CLICKED("Finish_Vertical_Next_section_Clicked"),
    FINISH_VERTICAL_BACK_CLICKED("Finish_Vertical_Back_to_outline_Clicked"),
    DISCUSSION_ALL_POSTS_CLICKED("Discussion_All_Posts_Clicked"),
    DISCUSSION_FOLLOWING_CLICKED("Discussion_Following_Clicked"),
    DISCUSSION_TOPIC_CLICKED("Discussion_Topic_Clicked"),
}

private enum class Key(val keyName: String) {
    COURSE_ID("course_id"),
    COURSE_NAME("course_name"),
    BLOCK_ID("block_id"),
    BLOCK_NAME("block_name"),
    TOPIC_ID("topic_id"),
    TOPIC_NAME("topic_name"),
    FORCE("force"),
    LABEL("label"),
    COURSE_COUNT("courses_count"),
}
