package com.raccoongang.discussion.presentation

import androidx.fragment.app.FragmentManager
import com.raccoongang.core.FragmentViewType
import com.raccoongang.discussion.domain.model.DiscussionComment

interface DiscussionRouter {

    fun navigateToDiscussionThread(
        fm: FragmentManager,
        action: String,
        courseId: String,
        topicId: String,
        title: String,
        viewType: FragmentViewType
    )

    fun navigateToDiscussionComments(
        fm: FragmentManager,
        thread: com.raccoongang.discussion.domain.model.Thread
    )

    fun navigateToDiscussionResponses(
        fm: FragmentManager,
        comment: DiscussionComment,
        isClosed: Boolean
    )

    fun navigateToAddThread(
        fm: FragmentManager,
        topicId: String,
        courseId: String
    )

    fun navigateToSearchThread(
        fm: FragmentManager,
        courseId: String
    )

}