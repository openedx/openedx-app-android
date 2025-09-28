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
    fun logScreenEvent(screenName: String, params: Map<String, Any?>)
}

enum class CourseAnalyticsEvent(val eventName: String, val biValue: String) {
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
    MORE_TAB(
        "Course:Handouts Tab",
        "edx.bi.app.course.handouts_tab"
    ),
    PROGRESS_TAB(
        "Course:Progress Tab",
        "edx.bi.app.course.progress_tab"
    ),
    OFFLINE_TAB(
        "Course:Offline Tab",
        "edx.bi.app.course.offline_tab"
    ),
    CONTENT_TAB(
        "Course:Content Tab",
        "edx.bi.app.course.content_tab"
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
    COURSE_CONTENT_TAB_CLICK(
        "Content Page:Section Click",
        "edx.bi.app.course.content.section.clicked"
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
    VIDEO_SHOW_COMPLETED(
        "Content Page:Show Completed Subsection Click",
        "edx.bi.app.course.content.show_completed_subsection.clicked"
    ),
    COURSE_CONTENT_VIDEO_CLICK(
        "Course:Video Clicked",
        "edx.bi.app.course.content.video.clicked"
    ),
    COURSE_CONTENT_ASSIGNMENT_CLICK(
        "Course:Assignment click",
        "edx.bi.app.course.content.assignment.clicked"
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
        "edx.bi.app.dates.pls_banner.viewed"
    ),
    PLS_SHIFT_BUTTON_CLICKED(
        "PLS:Shift Button Clicked",
        "edx.bi.app.dates.pls_banner.shift_dates.clicked"
    ),
    PLS_SHIFT_DATES_SUCCESS(
        "PLS:Shift Dates Success",
        "edx.bi.app.dates.pls_banner.shift_dates.success"
    ),
    DATES_CALENDAR_SYNC_TOGGLE(
        "Dates:CalendarSync Toggle Clicked",
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
    ASSIGNMENT_CLICKED(
        "Course:Assignment Tab.Assignment Clicked",
        "edx.bi.app.course.assignment_tab.assignment.clicked"
    ),
}

enum class CourseAnalyticsKey(val key: String) {
    NAME("name"),
    COURSE_ID("course_id"),
    COURSE_NAME("course_name"),
    OPEN_IN_BROWSER("open_in_browser_url"),
    COMPONENT("component"),
    VIDEO_PLAYER("video_player"),
    ENROLLMENT_MODE("enrollment_mode"),
    PACING("pacing"),
    SCREEN_NAME("screen_name"),
    BANNER_TYPE("banner_type"),
    CATEGORY("category"),
    SUCCESS("success"),
    LINK("link"),
    SUPPORTED("supported"),
    BLOCK_ID("block_id"),
    TAB_NAME("tab_name"),
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
    SNACKBAR_TYPE("snackbar_type"),
    COURSE_DATES("course_dates"),
    SELF_PACED("self"),
    INSTRUCTOR_PACED("instructor"),
}

enum class CalendarSyncDialog(
    val dialog: String,
    private val positiveAction: String,
    private val negativeAction: String,
) {
    PERMISSION("device_permission", "allow", "donot_allow"),
    ADD("add_calendar", "add", "cancel"),
    REMOVE("remove_calendar", "remove", "cancel"),
    UPDATE("update_calendar", "update", "remove"),
    CONFIRMED("events_added", "view_event", "done");

    fun getBuildMap(action: Boolean): Map<String, Any> {
        return buildMap {
            put(CourseAnalyticsKey.DIALOG.key, dialog)
            put(CourseAnalyticsKey.ACTION.key, if (action) positiveAction else negativeAction)
        }
    }
}

enum class CalendarSyncSnackbar(private val snackbar: String) {
    ADDED("added"),
    REMOVED("removed"),
    UPDATED("updated");

    fun getBuildMap(): Map<String, Any> {
        return buildMap {
            put(CourseAnalyticsKey.SNACKBAR_TYPE.key, snackbar)
        }
    }
}
