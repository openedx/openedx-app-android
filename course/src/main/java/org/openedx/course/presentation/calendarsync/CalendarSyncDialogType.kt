package org.openedx.course.presentation.calendarsync

import org.openedx.course.R
import org.openedx.core.R as CoreR

enum class CalendarSyncDialogType(
    val titleResId: Int = 0,
    val messageResId: Int = 0,
    val positiveButtonResId: Int = 0,
    val negativeButtonResId: Int = 0,
) {
    SYNC_DIALOG(
        titleResId = R.string.course_title_add_course_calendar,
        messageResId = R.string.course_message_add_course_calendar,
        positiveButtonResId = CoreR.string.core_ok,
        negativeButtonResId = CoreR.string.core_cancel
    ),
    UN_SYNC_DIALOG(
        titleResId = R.string.course_title_remove_course_calendar,
        messageResId = R.string.course_message_remove_course_calendar,
        positiveButtonResId = R.string.course_label_remove,
        negativeButtonResId = CoreR.string.core_cancel
    ),
    PERMISSION_DIALOG(
        titleResId = R.string.course_title_request_calendar_permission,
        messageResId = R.string.course_message_request_calendar_permission,
        positiveButtonResId = CoreR.string.core_ok,
        negativeButtonResId = R.string.course_label_do_not_allow
    ),
    EVENTS_DIALOG(
        messageResId = R.string.course_message_course_calendar_added,
        positiveButtonResId = R.string.course_label_view_events,
        negativeButtonResId = R.string.course_label_done
    ),
    OUT_OF_SYNC_DIALOG(
        titleResId = R.string.course_title_calendar_out_of_date,
        messageResId = R.string.course_message_calendar_out_of_date,
        positiveButtonResId = R.string.course_label_update_now,
        negativeButtonResId = R.string.course_label_remove_course_calendar,
    ),
    LOADING_DIALOG(
        titleResId = R.string.course_title_syncing_calendar
    ),
    NONE;
}
