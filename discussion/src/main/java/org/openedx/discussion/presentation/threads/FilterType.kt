package org.openedx.discussion.presentation.threads

import org.openedx.discussion.R

enum class FilterType(
    val textRes: Int,
    val value: String
) {
    ALL_POSTS(textRes = R.string.discussion_all_posts, value = "all_posts"),
    UNREAD(textRes = R.string.discussion_unread, value = "unread"),
    UNANSWERED(textRes = R.string.discussion_unanswered, value = "unanswered");

    companion object {
        const val type = "filter_type"
    }
}
