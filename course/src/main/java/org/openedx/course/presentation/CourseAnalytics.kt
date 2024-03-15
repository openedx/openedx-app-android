package org.openedx.course.presentation

interface CourseAnalytics {
    fun sequentialClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String,
    )

    fun nextBlockClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String,
    )

    fun prevBlockClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String,
    )

    fun finishVerticalClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String,
    )

    fun finishVerticalNextClickedEvent(
        courseId: String,
        courseName: String,
        blockId: String,
        blockName: String,
    )

    fun finishVerticalBackClickedEvent(courseId: String, courseName: String)
    fun logEvent(event: String, params: Map<String, Any?>)
}

enum class CourseAnalyticEvent(val event: String) {

    COURSE_ENROLL_CLICKED("Discovery:Course Enroll Clicked"),
    COURSE_ENROLL_SUCCESS("Discovery:Course Enroll Success"),
    COURSE_INFO("Discovery:Course Info"),

    DASHBOARD("Course:Dashboard"),
    HOME_TAB("Course:Home Tab"),
    VIDEOS_TAB("Course:Videos Tab"),
    DISCUSSION_TAB("Course:Discussion Tab"),
    DATES_TAB("Course:Dates Tab"),
    HANDOUTS_TAB("Course:Handouts Tab"),
    ANNOUNCEMENTS("Course:Announcements"),
    HANDOUTS("Course:Handouts"),
    UNIT_DETAIL("Course:Unit Detail"),
    VIEW_CERTIFICATE("Course:View Certificate"),
    RESUME_COURSE_CLICKED("Course:Resume Course Clicked"),

    VIDEO_LOADED("Video:Loaded"),
    VIDEO_CHANGE_SPEED("Video:Change Speed"),
    VIDEO_PLAYED("Video:Played"),
    VIDEO_PAUSED("Video:Paused"),
    VIDEO_SEEKED("Video:Seeked"),
    VIDEO_COMPLETED("Video:Completed"),


    CAST_CONNECTED("Cast:Connected"),
    CAST_DISCONNECTED("Cast:Disconnected"),

    DATES_COURSE_COMPONENT_TAPPED("Dates:Course Component Tapped"),
    DATES_UNSUPPORTED_COMPONENT_TAPPED("Dates:Unsupported Component Tapped"),
    PLS_BANNER_VIEWED("PLS:Banner Viewed"),
    PLS_SHIFT_BUTTON_TAPPED("PLS:Shift Button Tapped"),
    PLS_SHIFT_DATES("PLS:Shift Dates"),

    DATES_CALENDAR_TOGGLE_ON("Dates:Calendar Toggle On"),
    DATES_CALENDAR_TOGGLE_OFF("Dates:Calendar Toggle Off"),
    DATES_CALENDAR_ACCESS_ALLOWED("Dates:Calendar Access Allowed"),
    DATES_CALENDAR_ACCESS_DONT_ALLOW("Dates:Calendar Access Don't Allow"),
    DATES_CALENDAR_ADD_DATES("Dates:Calendar Add Dates"),
    DATES_CALENDAR_ADD_CANCELLED("Dates:Calendar Add Cancelled"),
    DATES_CALENDAR_REMOVE_DATES("Dates:Calendar Remove Dates"),
    DATES_CALENDAR_REMOVE_CANCELLED("Dates:Calendar Remove Cancelled"),
    DATES_CALENDAR_ADD_CONFIRMATION("Dates:Calendar Add Confirmation"),
    DATES_CALENDAR_VIEW_EVENTS("Dates:Calendar View Events"),
    DATES_CALENDAR_SYNC_UPDATE_DATES("Dates:Calendar Sync Update Dates"),
    DATES_CALENDAR_SYNC_REMOVE_CALENDAR("Dates:Calendar Sync Remove Calendar"),
    DATES_CALENDAR_ADD_DATES_SUCCESS("Dates:Calendar Add Dates Success"),
    DATES_CALENDAR_REMOVE_DATES_SUCCESS("Dates:Calendar Remove Dates Success"),
    DATES_CALENDAR_UPDATE_DATES_SUCCESS("Dates:Calendar Update Dates Success"),
}

enum class CourseAnalyticValue(val biValue: String) {

    COURSE_ENROLL_CLICKED("edx.bi.app.course.enroll.clicked"),
    COURSE_ENROLL_SUCCESS("edx.bi.app.course.enroll.success"),
    COURSE_INFO("edx.bi.app.course.info"),

    DASHBOARD("edx.bi.app.course.dashboard"),
    HOME_TAB("edx.bi.app.course.home_tab"),
    VIDEOS_TAB(" edx.bi.app.course.video_tab"),
    DISCUSSION_TAB("edx.bi.app.course.discussion_tab"),
    DATES_TAB("edx.bi.app.course.dates_tab"),
    HANDOUTS_TAB("edx.bi.app.course.handouts_tab"),
    ANNOUNCEMENTS("edx.bi.app.course.announcements"),
    HANDOUTS("edx.bi.app.course.handouts"),
    UNIT_DETAIL("edx.bi.app.course.unit_detail"),

    VIEW_CERTIFICATE("edx.bi.app.course.view_certificate.clicked"),
    RESUME_COURSE_CLICKED("edx.bi.app.course.resume_course.clicked"),

    VIDEO_LOADED("edx.bi.app.videos.loaded"),
    VIDEO_CHANGE_SPEED("edx.bi.app.videos.speed.changed"),
    VIDEO_PLAYED("edx.bi.app.videos.played"),
    VIDEO_PAUSED("edx.bi.app.videos.paused"),
    VIDEO_SEEKED("edx.bi.app.videos.position.changed"),
    VIDEO_COMPLETED("edx.bi.app.videos.completed"),

    GOOGLE_CAST("google_cast"),
    CAST_CONNECTED("edx.bi.app.cast.connected"),
    CAST_DISCONNECTED("edx.bi.app.cast.disconnected"),

    COURSE_DATES("course_dates"),
    DATES_COURSE_COMPONENT_TAPPED("edx.bi.app.coursedates.component.tapped"),
    DATES_UNSUPPORTED_COMPONENT_TAPPED("edx.bi.app.coursedates.unsupported.component.tapped"),
    PLS_BANNER_VIEWED("edx.bi.app.coursedates.pls_banner.viewed"),
    PLS_SHIFT_BUTTON_TAPPED("edx.bi.app.coursedates.pls_banner.shift_button.tapped"),
    PLS_SHIFT_DATES("edx.bi.app.coursedates.pls_banner.shift_dates"),

    DATES_CALENDAR_TOGGLE_ON("edx.bi.app.calendar.toggle_on"),
    DATES_CALENDAR_TOGGLE_OFF("edx.bi.app.calendar.toggle_off"),
    DATES_CALENDAR_ACCESS_ALLOWED("edx.bi.app.calendar.access_ok"),
    DATES_CALENDAR_ACCESS_DONT_ALLOW("edx.bi.app.calendar.access_dont_allow"),
    DATES_CALENDAR_ADD_DATES("edx.bi.app.calendar.add_ok"),
    DATES_CALENDAR_ADD_CANCELLED("edx.bi.app.calendar.add_cancel"),
    DATES_CALENDAR_REMOVE_DATES("edx.bi.app.calendar.remove_ok"),
    DATES_CALENDAR_REMOVE_CANCELLED("edx.bi.app.calendar.remove_cancel"),
    DATES_CALENDAR_ADD_CONFIRMATION("edx.bi.app.calendar.confirmation_done"),
    DATES_CALENDAR_VIEW_EVENTS("edx.bi.app.calendar.confirmation_view_events"),
    DATES_CALENDAR_SYNC_UPDATE_DATES("edx.bi.app.calendar.sync_update"),
    DATES_CALENDAR_SYNC_REMOVE_CALENDAR("edx.bi.app.calendar.sync_remove"),
    DATES_CALENDAR_ADD_DATES_SUCCESS("edx.bi.app.calendar.add_success"),
    DATES_CALENDAR_REMOVE_DATES_SUCCESS("edx.bi.app.calendar.remove_success"),
    DATES_CALENDAR_UPDATE_DATES_SUCCESS("edx.bi.app.calendar.update_success"),
}

enum class CourseAnalyticKey(val key: String) {
    NAME("name"),
    COURSE_ID("course_id"),
    OPEN_IN_BROWSER("open_in_browser_url"),
    COMPONENT("component"),
    VIDEO_PLAYER("videoplayer"),
    ENROLLMENT_MODE("enrollment_mode"),
    PACING("pacing"),
    SCREEN_NAME("screen_name"),
    BANNER_TYPE("banner_type"),
    CATEGORY("category"),
    SUCCESS("success"),
    LINK("link"),
    BLOCK_ID("block_id"),
    BLOCK_TYPE("block_type"),
    PLAY_MEDIUM("play_medium"),
    NATIVE("native"),
    YOUTUBE("youtube"),
    GOOGLE_CAST("google_cast"),
    CURRENT_TIME("current_time"),
    SKIP_INTERVAL("requested_skip_interval"),
    SPEED("speed"),
    NAVIGATION("navigation"),
}
