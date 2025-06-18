package org.openedx.core

enum class NoContentScreenType(
    val iconResId: Int,
    val messageResId: Int,
) {
    COURSE_OUTLINE(
        iconResId = R.drawable.core_ic_no_content,
        messageResId = R.string.core_no_course_content
    ),
    COURSE_VIDEOS(
        iconResId = R.drawable.core_ic_no_videos,
        messageResId = R.string.core_no_videos
    ),
    COURSE_DATES(
        iconResId = R.drawable.core_ic_no_content,
        messageResId = R.string.core_no_dates
    ),
    COURSE_DISCUSSIONS(
        iconResId = R.drawable.core_ic_no_content,
        messageResId = R.string.core_no_discussion
    ),
    COURSE_HANDOUTS(
        iconResId = R.drawable.core_ic_no_handouts,
        messageResId = R.string.core_no_handouts
    ),
    COURSE_ANNOUNCEMENTS(
        iconResId = R.drawable.core_ic_no_announcements,
        messageResId = R.string.core_no_announcements
    ),
    COURSE_PROGRESS(
        iconResId = R.drawable.core_ic_no_content,
        messageResId = R.string.core_no_progress
    ),
}
