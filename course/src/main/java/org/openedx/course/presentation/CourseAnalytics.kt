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

enum class CourseAnalyticsEvent(val eventName: String, val biValue: String) {
    COURSE_ENROLL_CLICKED(
        "Discovery:Course Enroll Clicked",
        "edx.bi.app.course.enroll.clicked"
    ),
    COURSE_ENROLL_SUCCESS(
        "Discovery:Course Enroll Success",
        "edx.bi.app.course.enroll.success"
    ),
    COURSE_INFO(
        "Discovery:Course Info",
        "edx.bi.app.course.info"
    ),
    DASHBOARD(
        "Course:Dashboard",
        "edx.bi.app.course.dashboard"
    ),
    HOME_TAB(
        "Course:Home Tab",
        "edx.bi.app.course.home_tab"
    ),
    VIDEOS_TAB(
        "Course:Videos Tab",
        "edx.bi.app.course.video_tab"
    ),
    DISCUSSION_TAB(
        "Course:Discussion Tab",
        "edx.bi.app.course.discussion_tab"
    ),
    DATES_TAB(
        "Course:Dates Tab",
        "edx.bi.app.course.dates_tab"
    ),
    HANDOUTS_TAB(
        "Course:Handouts Tab",
        "edx.bi.app.course.handouts_tab"
    ),
    ANNOUNCEMENTS(
        "Course:Announcements",
        "edx.bi.app.course.announcements"
    ),
    HANDOUTS(
        "Course:Handouts",
        "edx.bi.app.course.handouts"
    ),
    UNIT_DETAIL(
        "Course:Unit Detail",
        "edx.bi.app.course.unit_detail"
    ),
    VIEW_CERTIFICATE(
        "Course:View Certificate Clicked",
        "edx.bi.app.course.view_certificate.clicked"
    ),
    RESUME_COURSE_CLICKED(
        "Course:Resume Course Clicked",
        "edx.bi.app.course.resume_course.clicked"
    ),
    VIDEO_LOADED(
        "Video:Loaded",
        "edx.bi.app.videos.loaded"
    ),
    VIDEO_CHANGE_SPEED(
        "Video:Change Speed",
        "edx.bi.app.videos.speed.changed"
    ),
    VIDEO_PLAYED(
        "Video:Played",
        "edx.bi.app.videos.played"
    ),
    VIDEO_PAUSED(
        "Video:Paused",
        "edx.bi.app.videos.paused"
    ),
    VIDEO_SEEKED(
        "Video:Seeked",
        "edx.bi.app.videos.position.changed"
    ),
    VIDEO_COMPLETED(
        "Video:Completed",
        "edx.bi.app.videos.completed"
    ),
    CAST_CONNECTED(
        "Cast:Connected",
        "edx.bi.app.cast.connected"
    ),
    CAST_DISCONNECTED(
        "Cast:Disconnected",
        "edx.bi.app.cast.disconnected"
    ),
    DATES_COURSE_COMPONENT_CLICKED(
        "Dates:Course Component Clicked",
        "edx.bi.app.dates.component.clicked"
    ),
    PLS_BANNER_VIEWED(
        "PLS:Banner Viewed",
        "edx.bi.app.coursedates.pls_banner.viewed"
    ),
    PLS_SHIFT_BUTTON_CLICKED(
        "PLS:Shift Button Clicked",
        "edx.bi.app.dates.pls_banner.shift_dates.clicked"
    ),
    PLS_SHIFT_DATES(
        "PLS:Shift Dates",
        "edx.bi.app.coursedates.pls_banner.shift_dates"
    ),
    DATES_CALENDAR_SYNC_TOGGLE(
        "Dates:CalendarSync Toggle",
        "edx.bi.app.dates.calendar_sync.toggle"
    ),
    DATES_CALENDAR_SYNC_DIALOG_ACTION(
        "Dates:CalendarSync Dialog Action",
        "edx.bi.app.dates.calendar_sync.dialog_action"
    ),
    DATES_CALENDAR_SYNC_SNACKBAR(
        "Dates:CalendarSync Snackbar",
        "edx.bi.app.dates.calendar_sync.snackbar"
    ),
}

enum class CourseAnalyticKey(val key: String) {
    NAME("name"),
    COURSE_ID("course_id"),
    COURSE_NAME("course_name"),
    CONVERSION("conversion"),
    OPEN_IN_BROWSER("open_in_browser_url"),
    COMPONENT("component"),
    VIDEO_PLAYER("video_player"),
    ENROLLMENT_MODE("mode"),
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
    COURSE_DATES("course_dates"),
}

enum class CalendarSyncDialog(
    val dialog: String,
    private val positiveAction: String,
    private val negativeAction: String,
) {
    PERMISSION("permission", "allow", "donot_allow"),
    ADD("add", "ok", "cancel"),
    REMOVE("remove", "ok", "cancel"),
    UPDATE("update", "update", "remove"),
    CONFIRMED("confirmed", "view_event", "done");

    fun getBuildMap(action: Boolean): Map<String, Any> {
        return buildMap {
            put(CourseAnalyticKey.DIALOG.key, dialog)
            put(CourseAnalyticKey.ACTION.key, if (action) positiveAction else negativeAction)
        }
    }
}

enum class CalendarSyncSnackbar(private val snackbar: String) {
    ADD("add"),
    REMOVE("remove"),
    UPDATE("update");

    fun getBuildMap(): Map<String, Any> {
        return buildMap {
            put(CourseAnalyticKey.SNACKBAR.key, snackbar)
        }
    }
}
