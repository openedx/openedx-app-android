package org.openedx.app

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.dashboard.presentation.DashboardAnalytics
import org.openedx.discovery.presentation.DiscoveryAnalytics
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.profile.presentation.ProfileAnalytics

class AnalyticsManager(context: Context) : DashboardAnalytics, AuthAnalytics, AppAnalytics,
    DiscoveryAnalytics, ProfileAnalytics, CourseAnalytics, DiscussionAnalytics {

    private val analytics = FirebaseAnalytics.getInstance(context)

    private fun logEvent(event: Event, params: Bundle = bundleOf()) {
        analytics.logEvent(event.eventName, params)
    }

    private fun setUserId(userId: Long) {
        analytics.setUserId(userId.toString())
    }

    override fun dashboardCourseClickedEvent(courseId: String, courseName: String) {
        logEvent(
            Event.DASHBOARD_COURSE_CLICKED,
            bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName
            )
        )
    }

    override fun userLoginEvent(method: String) {
        logEvent(
            Event.USER_LOGIN,
            bundleOf(
                Key.METHOD.keyName to method
            )
        )
    }

    override fun signUpClickedEvent() {
        logEvent(Event.SIGN_UP_CLICKED)
    }

    override fun createAccountClickedEvent(provider: String) {
        logEvent(
            Event.CREATE_ACCOUNT_CLICKED,
            bundleOf(Key.PROVIDER.keyName to provider)
        )
    }

    override fun registrationSuccessEvent(provider: String) {
        logEvent(
            Event.REGISTRATION_SUCCESS,
            bundleOf(Key.PROVIDER.keyName to provider)
        )
    }

    override fun forgotPasswordClickedEvent() {
        logEvent(Event.FORGOT_PASSWORD_CLICKED)
    }

    override fun resetPasswordClickedEvent(success: Boolean) {
        logEvent(
            Event.RESET_PASSWORD_CLICKED, bundleOf(
                Key.SUCCESS.keyName to success
            )
        )
    }

    override fun logoutEvent(force: Boolean) {
        logEvent(
            Event.USER_LOGOUT, bundleOf(
                Key.FORCE.keyName to force
            )
        )
    }

    override fun discoveryTabClickedEvent() {
        logEvent(Event.DISCOVERY_TAB_CLICKED)
    }

    override fun dashboardTabClickedEvent() {
        logEvent(Event.DASHBOARD_TAB_CLICKED)
    }

    override fun programsTabClickedEvent() {
        logEvent(Event.PROGRAMS_TAB_CLICKED)
    }

    override fun profileTabClickedEvent() {
        logEvent(Event.PROFILE_TAB_CLICKED)
    }

    override fun setUserIdForSession(userId: Long) {
        setUserId(userId)
    }

    override fun discoverySearchBarClickedEvent() {
        logEvent(Event.DISCOVERY_SEARCH_BAR_CLICKED)
    }

    override fun discoveryCourseSearchEvent(label: String, coursesCount: Int) {
        logEvent(
            Event.DISCOVERY_COURSE_SEARCH, bundleOf(
                Key.LABEL.keyName to label,
                Key.COURSE_COUNT.keyName to coursesCount
            )
        )
    }

    override fun discoveryCourseClickedEvent(courseId: String, courseName: String) {
        logEvent(
            Event.DISCOVERY_COURSE_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName
            )
        )
    }

    override fun profileEditClickedEvent() {
        logEvent(Event.PROFILE_EDIT_CLICKED)
    }

    override fun profileEditDoneClickedEvent() {
        logEvent(Event.PROFILE_EDIT_DONE_CLICKED)
    }

    override fun profileDeleteAccountClickedEvent() {
        logEvent(Event.PROFILE_DELETE_ACCOUNT_CLICKED)
    }

    override fun profileVideoSettingsClickedEvent() {
        logEvent(Event.PROFILE_VIDEO_SETTINGS_CLICKED)
    }

    override fun privacyPolicyClickedEvent() {
        logEvent(Event.PRIVACY_POLICY_CLICKED)
    }

    override fun cookiePolicyClickedEvent() {
        logEvent(Event.COOKIE_POLICY_CLICKED)
    }

    override fun emailSupportClickedEvent() {
        logEvent(Event.EMAIL_SUPPORT_CLICKED)
    }

    override fun courseEnrollClickedEvent(courseId: String, courseName: String) {
        logEvent(
            Event.COURSE_ENROLL_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName,
            )
        )
    }

    override fun courseEnrollSuccessEvent(courseId: String, courseName: String) {
        logEvent(
            Event.COURSE_ENROLL_SUCCESS, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName,
            )
        )
    }

    override fun viewCourseClickedEvent(courseId: String, courseName: String) {
        logEvent(
            Event.VIEW_COURSE_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName,
            )
        )
    }

    override fun resumeCourseTappedEvent(courseId: String, courseName: String, blockId: String) {
        logEvent(
            Event.RESUME_COURSE_TAPPED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName,
                Key.BLOCK_ID.keyName to blockId
            )
        )
    }

    override fun sequentialClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String
    ) {
        logEvent(
            Event.SEQUENTIAL_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName,
                Key.BLOCK_ID.keyName to blockId,
                Key.BLOCK_NAME.keyName to blockName,
            )
        )
    }

    override fun verticalClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String
    ) {
        logEvent(
            Event.VERTICAL_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName,
                Key.BLOCK_ID.keyName to blockId,
                Key.BLOCK_NAME.keyName to blockName,
            )
        )
    }

    override fun nextBlockClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String
    ) {
        logEvent(
            Event.NEXT_BLOCK_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName,
                Key.BLOCK_ID.keyName to blockId,
                Key.BLOCK_NAME.keyName to blockName,
            )
        )
    }

    override fun prevBlockClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String
    ) {
        logEvent(
            Event.PREV_BLOCK_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName,
                Key.BLOCK_ID.keyName to blockId,
                Key.BLOCK_NAME.keyName to blockName,
            )
        )
    }

    override fun finishVerticalClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String
    ) {
        logEvent(
            Event.FINISH_VERTICAL_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName,
                Key.BLOCK_ID.keyName to blockId,
                Key.BLOCK_NAME.keyName to blockName,
            )
        )
    }

    override fun finishVerticalNextClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String
    ) {
        logEvent(
            Event.FINISH_VERTICAL_NEXT_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName,
                Key.BLOCK_ID.keyName to blockId,
                Key.BLOCK_NAME.keyName to blockName,
            )
        )
    }

    override fun finishVerticalBackClickedEvent(courseId: String, courseName: String) {
        logEvent(
            Event.FINISH_VERTICAL_BACK_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName
            )
        )
    }

    override fun courseTabClickedEvent(courseId: String, courseName: String) {
        logEvent(
            Event.COURSE_TAB_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName
            )
        )
    }

    override fun videoTabClickedEvent(courseId: String, courseName: String) {
        logEvent(
            Event.VIDEO_TAB_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName
            )
        )
    }

    override fun discussionTabClickedEvent(courseId: String, courseName: String) {
        logEvent(
            Event.DISCUSSION_TAB_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName
            )
        )
    }

    override fun datesTabClickedEvent(courseId: String, courseName: String) {
        logEvent(
            Event.DATES_TAB_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName
            )
        )
    }

    override fun handoutsTabClickedEvent(courseId: String, courseName: String) {
        logEvent(
            Event.HANDOUTS_TAB_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName
            )
        )
    }

    override fun discussionAllPostsClickedEvent(courseId: String, courseName: String) {
        logEvent(
            Event.DISCUSSION_ALL_POSTS_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName
            )
        )
    }

    override fun discussionFollowingClickedEvent(courseId: String, courseName: String) {
        logEvent(
            Event.DISCUSSION_FOLLOWING_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName
            )
        )
    }

    override fun discussionTopicClickedEvent(
        courseId: String,
        courseName: String,
        topicId: String,
        topicName: String
    ) {
        logEvent(
            Event.DISCUSSION_TOPIC_CLICKED, bundleOf(
                Key.COURSE_ID.keyName to courseId,
                Key.COURSE_NAME.keyName to courseName,
                Key.TOPIC_ID.keyName to topicId,
                Key.TOPIC_NAME.keyName to topicName
            )
        )
    }

}

private enum class Event(val eventName: String) {
    USER_LOGIN("User_Login"),
    SIGN_UP_CLICKED("Sign_up_Clicked"),
    CREATE_ACCOUNT_CLICKED("Create_Account_Clicked"),
    REGISTRATION_SUCCESS("Registration_Success"),
    USER_LOGOUT("User_Logout"),
    FORGOT_PASSWORD_CLICKED("Forgot_password_Clicked"),
    RESET_PASSWORD_CLICKED("Reset_password_Clicked"),
    DISCOVERY_TAB_CLICKED("Main_Discovery_tab_Clicked"),
    DASHBOARD_TAB_CLICKED("Main_Dashboard_tab_Clicked"),
    PROGRAMS_TAB_CLICKED("Main_Programs_tab_Clicked"),
    PROFILE_TAB_CLICKED("Main_Profile_tab_Clicked"),
    DISCOVERY_SEARCH_BAR_CLICKED("Discovery_Search_Bar_Clicked"),
    DISCOVERY_COURSE_SEARCH("Discovery_Courses_Search"),
    DISCOVERY_COURSE_CLICKED("Discovery_Course_Clicked"),
    DASHBOARD_COURSE_CLICKED("Dashboard_Course_Clicked"),
    PROFILE_EDIT_CLICKED("Profile_Edit_Clicked"),
    PROFILE_EDIT_DONE_CLICKED("Profile_Edit_Done_Clicked"),
    PROFILE_DELETE_ACCOUNT_CLICKED("Profile_Delete_Account_Clicked"),
    PROFILE_VIDEO_SETTINGS_CLICKED("Profile_Video_settings_Clicked"),
    PRIVACY_POLICY_CLICKED("Privacy_Policy_Clicked"),
    COOKIE_POLICY_CLICKED("Cookie_Policy_Clicked"),
    EMAIL_SUPPORT_CLICKED("Email_Support_Clicked"),
    COURSE_ENROLL_CLICKED("Course_Enroll_Clicked"),
    COURSE_ENROLL_SUCCESS("Course_Enroll_Success"),
    VIEW_COURSE_CLICKED("View_Course_Clicked"),
    RESUME_COURSE_TAPPED("Resume_Course_Tapped"),
    SEQUENTIAL_CLICKED("Sequential_Clicked"),
    VERTICAL_CLICKED("Vertical_Clicked"),
    NEXT_BLOCK_CLICKED("Next_Block_Clicked"),
    PREV_BLOCK_CLICKED("Prev_Block_Clicked"),
    FINISH_VERTICAL_CLICKED("Finish_Vertical_Clicked"),
    FINISH_VERTICAL_NEXT_CLICKED("Finish_Vertical_Next_section_Clicked"),
    FINISH_VERTICAL_BACK_CLICKED("Finish_Vertical_Back_to_outline_Clicked"),
    COURSE_TAB_CLICKED("Course_Outline_Course_tab_Clicked"),
    VIDEO_TAB_CLICKED("Course_Outline_Videos_tab_Clicked"),
    DISCUSSION_TAB_CLICKED("Course_Outline_Discussion_tab_Clicked"),
    DATES_TAB_CLICKED("Course_Outline_Dates_tab_Clicked"),
    HANDOUTS_TAB_CLICKED("Course_Outline_Handouts_tab_Clicked"),
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
    METHOD("method"),
    SUCCESS("success"),
    PROVIDER("provider"),
    FORCE("force"),
    LABEL("label"),
    COURSE_COUNT("courses_count"),
}
