package org.openedx.discussion.domain.interactor

import org.openedx.discussion.data.repository.DiscussionRepository
import org.openedx.discussion.domain.model.DiscussionComment

class DiscussionInteractor(
    private val repository: DiscussionRepository
) {

    suspend fun getCourseTopics(courseId: String) = repository.getCourseTopics(courseId)

    fun getCachedTopics(courseId: String) = repository.getCachedTopics(courseId)

    suspend fun getAllThreads(courseId: String, orderBy: String, view: String? = null, page: Int) =
        repository.getCourseThreads(courseId, null, null, orderBy, view, page)

    suspend fun getFollowingThreads(
        courseId: String,
        following: Boolean,
        orderBy: String,
        view: String? = null,
        page: Int
    ) =
        repository.getCourseThreads(courseId, following, null, orderBy, view, page)

    suspend fun getThreads(
        courseId: String,
        topicId: String,
        orderBy: String,
        view: String? = null,
        page: Int
    ) =
        repository.getCourseThreads(courseId, null, topicId, orderBy, view, page)

    suspend fun getThread(threadId: String, courseId: String, topicId: String) =
        repository.getCourseThread(threadId, courseId, topicId)

    suspend fun searchThread(courseId: String, query: String, page: Int) =
        repository.searchThread(courseId, query, page)

    suspend fun getThreadComments(threadId: String, page: Int) =
        repository.getThreadComments(threadId, page)

    suspend fun getResponse(responseId: String): DiscussionComment =
        repository.getResponse(responseId)

    suspend fun getThreadQuestionComments(threadId: String, endorsed: Boolean, page: Int) =
        repository.getThreadQuestionComments(threadId, endorsed, page)

    suspend fun setThreadRead(
        threadId: String
    ) = repository.setThreadRead(threadId)

    suspend fun setThreadVoted(
        threadId: String,
        isVoted: Boolean
    ) = repository.setThreadVoted(threadId, isVoted)

    suspend fun setThreadFlagged(
        threadId: String,
        abuseFlagged: Boolean
    ) = repository.setThreadFlagged(threadId, abuseFlagged)

    suspend fun setThreadFollowed(
        threadId: String,
        following: Boolean
    ) = repository.setThreadFollowed(threadId, following)

    suspend fun setCommentVoted(
        commentId: String,
        isVoted: Boolean
    ) = repository.setCommentVoted(commentId, isVoted)

    suspend fun setCommentFlagged(
        commentId: String,
        abuseFlagged: Boolean
    ) = repository.setCommentFlagged(commentId, abuseFlagged)

    suspend fun getCommentsResponses(commentId: String, page: Int) =
        repository.getCommentsResponses(commentId, page)

    suspend fun createComment(
        threadId: String,
        rawBody: String,
        parentId: String?
    ) = repository.createComment(threadId, rawBody, parentId)

    suspend fun createThread(
        topicId: String,
        courseId: String,
        type: String,
        title: String,
        rawBody: String,
        follow: Boolean
    ) = repository.createThread(topicId, courseId, type, title, rawBody, follow)

    suspend fun markBlocksCompletion(courseId: String, blocksId: List<String>) =
        repository.markBlocksCompletion(courseId, blocksId)
}
