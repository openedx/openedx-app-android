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
    PLS_BANNER_VIEWED("PLS:Banner Viewed"),
    PLS_SHIFT_BUTTON_TAPPED("PLS:Shift Button Tapped"),
    PLS_SHIFT_DATES("PLS:Shift Dates"),

    DATES_CALENDAR_SYNC_TOGGLE("Dates:CalendarSync Toggle"),
    DATES_CALENDAR_SYNC_DIALOG_ACTION("Dates:CalendarSync Dialog Action"),
    DATES_CALENDAR_SYNC_SNACKBAR("Dates:CalendarSync Snackbar"),
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
    PLS_BANNER_VIEWED("edx.bi.app.coursedates.pls_banner.viewed"),
    PLS_SHIFT_BUTTON_TAPPED("edx.bi.app.coursedates.pls_banner.shift_button.tapped"),
    PLS_SHIFT_DATES("edx.bi.app.coursedates.pls_banner.shift_dates"),

    DATES_CALENDAR_SYNC_TOGGLE("edx.bi.app.dates.calendar_sync.toggle"),
    DATES_CALENDAR_SYNC_DIALOG_ACTION("edx.bi.app.dates.calendar_sync.dialog_action"),
    DATES_CALENDAR_SYNC_SNACKBAR("edx.bi.app.dates.calendar_sync.snackbar"),
}

enum class CourseAnalyticKey(val key: String) {
    NAME("name"),
    COURSE_ID("course_id"),
    COURSE_NAME("course_name"),
    CONVERSION("conversion"),
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
    SUPPORTED("supported"),
    BLOCK_ID("block_id"),
    BLOCK_NAME("block_name"),
    BLOCK_TYPE("block_type"),
    PLAY_MEDIUM("play_medium"),
    NATIVE("native"),
    YOUTUBE("youtube"),
    GOOGLE_CAST("google_cast"),
    CURRENT_TIME("current_time"),
    SKIP_INTERVAL("requested_skip_interval"),
    SPEED("speed"),
    NAVIGATION("navigation"),
    DIALOG("dialog"),
    ACTION("action"),
    ON("on"),
    OFF("off"),
    SNACKBAR("snackbar"),
}

enum class CalendarSyncDialog(
    val dialog: String,
    val positiveAction: String,
    val negativeAction: String,
) {
    PERMISSION("permission", "allow", "donot_allow"),
    ADD("add", "ok", "cancel"),
    REMOVE("remove", "ok", "cancel"),
    UPDATE("update", "update", "remove"),
    CONFIRMED("confirmed", "done", "view_event");

    fun getBuildMap(action: Boolean): Map<String, Any> {
        return buildMap {
            put(CourseAnalyticKey.DIALOG.key, dialog)
            put(CourseAnalyticKey.ACTION.key, if (action) positiveAction else negativeAction)
        }
    }
}

enum class CalendarSyncSnackbar(val snackbar: String) {
    ADD("add"),
    REMOVE("remove"),
    UPDATE("update");

    fun getBuildMap(): Map<String, Any> {
        return buildMap {
            put(CourseAnalyticKey.SNACKBAR.key, snackbar)
        }
    }
}
