package com.raccoongang.discussion.presentation.threads

import com.raccoongang.discussion.R

enum class SortType(
    val textRes: Int,
    val queryParam: String
) {
    LAST_ACTIVITY_AT(textRes = R.string.discussion_recent_activity, queryParam = "last_activity_at"),
    COMMENT_COUNT(textRes = R.string.discussion_most_activity, queryParam = "comment_count"),
    VOTE_COUNT(textRes = R.string.discussion_most_votes, queryParam = "vote_count");

    companion object {
        const val type = "sort_type"
    }
}