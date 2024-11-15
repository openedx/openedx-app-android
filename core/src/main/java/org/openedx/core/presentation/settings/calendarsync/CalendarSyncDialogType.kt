package org.openedx.core.presentation.settings.calendarsync

import org.openedx.core.R

enum class CalendarSyncDialogType(
    val titleResId: Int = 0,
    val messageResId: Int = 0,
    val positiveButtonResId: Int = 0,
    val negativeButtonResId: Int = 0,
) {
    SYNC_DIALOG(
        titleResId = R.string.core_title_add_course_calendar,
        messageResId = R.string.core_message_add_course_calendar,
        positiveButtonResId = R.string.core_ok,
        negativeButtonResId = R.string.core_cancel
    ),
    UN_SYNC_DIALOG(
        titleResId = R.string.core_title_remove_course_calendar,
        messageResId = R.string.core_message_remove_course_calendar,
        positiveButtonResId = R.string.core_label_remove,
        negativeButtonResId = R.string.core_cancel
    ),
    PERMISSION_DIALOG(
        titleResId = R.string.core_title_request_calendar_permission,
        messageResId = R.string.core_message_request_calendar_permission,
        positiveButtonResId = R.string.core_ok,
        negativeButtonResId = R.string.core_label_do_not_allow
    ),
    EVENTS_DIALOG(
        messageResId = R.string.core_message_course_calendar_added,
        positiveButtonResId = R.string.core_label_view_events,
        negativeButtonResId = R.string.core_label_done
    ),
    OUT_OF_SYNC_DIALOG(
        titleResId = R.string.core_title_calendar_out_of_date,
        messageResId = R.string.core_message_calendar_out_of_date,
        positiveButtonResId = R.string.core_label_update_now,
        negativeButtonResId = R.string.core_label_remove_course_calendar,
    ),
    LOADING_DIALOG(
        titleResId = R.string.core_title_syncing_calendar
    ),
    NONE
}
