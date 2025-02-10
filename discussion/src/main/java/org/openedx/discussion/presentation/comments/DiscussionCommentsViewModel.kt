package org.openedx.discussion.presentation.comments

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.R
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.domain.model.DiscussionType
import org.openedx.discussion.system.notifier.DiscussionCommentAdded
import org.openedx.discussion.system.notifier.DiscussionCommentDataChanged
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.discussion.system.notifier.DiscussionThreadDataChanged
import org.openedx.foundation.extension.isInternetError
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.presentation.SingleEventLiveData
import org.openedx.foundation.presentation.UIMessage
import org.openedx.foundation.system.ResourceManager

class DiscussionCommentsViewModel(
    private val interactor: DiscussionInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: DiscussionNotifier,
    thread: org.openedx.discussion.domain.model.Thread,
) : BaseViewModel() {

    val title = resourceManager.getString(thread.type.resId)

    var thread: org.openedx.discussion.domain.model.Thread
        private set
    private var commentCount = 0

    private val _uiState = MutableLiveData<DiscussionCommentsUIState>()
    val uiState: LiveData<DiscussionCommentsUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _canLoadMore = MutableLiveData<Boolean>()
    val canLoadMore: LiveData<Boolean>
        get() = _canLoadMore

    private val _isUpdating = MutableLiveData<Boolean>()
    val isUpdating: LiveData<Boolean>
        get() = _isUpdating

    private val comments = mutableListOf<DiscussionComment>()
    private var page = 1
    private var isLoading = false

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect {
                if (it is DiscussionCommentAdded) {
                    if (page == -1) {
                        comments.add(it.comment)
                        _uiState.value = DiscussionCommentsUIState.Success(
                            thread,
                            comments.toList(),
                            commentCount
                        )
                    } else {
                        _uiMessage.value =
                            UIMessage.ToastMessage(
                                resourceManager.getString(org.openedx.discussion.R.string.discussion_comment_added)
                            )
                    }
                    thread = thread.copy(commentCount = thread.commentCount + 1)
                    sendThreadUpdated()
                } else if (it is DiscussionCommentDataChanged) {
                    val index = comments.indexOfFirst { innerComment ->
                        innerComment.id == it.discussionComment.id
                    }
                    if (index >= 0) {
                        comments[index] = it.discussionComment
                        _uiState.value = DiscussionCommentsUIState.Success(
                            thread,
                            comments.toList(),
                            commentCount
                        )
                    }
                }
            }
        }
    }

    init {
        this.thread = thread
        getThreadComments()
    }

    private fun sendThreadUpdated() {
        viewModelScope.launch {
            notifier.send(DiscussionThreadDataChanged(thread))
        }
    }

    private fun internalLoadComments(markReadIfSuccessful: Boolean) {
        viewModelScope.launch {
            try {
                isLoading = true
                val response = if (thread.type == DiscussionType.DISCUSSION) {
                    interactor.getThreadComments(thread.id, page)
                } else {
                    interactor.getThreadQuestionComments(thread.id, thread.hasEndorsed, page)
                }
                if (response.pagination.next.isNotEmpty() && page != response.pagination.numPages) {
                    _canLoadMore.value = true
                    page++
                } else {
                    _canLoadMore.value = false
                    page = -1
                }
                commentCount = response.pagination.count
                comments.addAll(response.results)
                _uiState.value =
                    DiscussionCommentsUIState.Success(thread, comments.toList(), commentCount)

                if (markReadIfSuccessful) {
                    markRead()
                }
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            } finally {
                isLoading = false
                _isUpdating.value = false
            }
        }
    }

    private fun markRead() {
        viewModelScope.launch {
            try {
                val response = interactor.setThreadRead(thread.id)
                thread = thread.copy(
                    read = response.read,
                    unreadCommentCount = response.unreadCommentCount
                )
                sendThreadUpdated()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getThreadComments() {
        _uiState.value = DiscussionCommentsUIState.Loading
        internalLoadComments(markReadIfSuccessful = true)
    }

    fun updateThreadComments() {
        _isUpdating.value = true
        page = 1
        comments.clear()
        internalLoadComments(markReadIfSuccessful = false)
    }

    fun fetchMore() {
        if (!isLoading && page != -1) {
            internalLoadComments(markReadIfSuccessful = false)
        }
    }

    fun setThreadUpvoted(vote: Boolean) {
        viewModelScope.launch {
            try {
                val response = interactor.setThreadVoted(thread.id, vote)
                thread = thread.copy(voted = response.voted, voteCount = response.voteCount)
                _uiState.value =
                    DiscussionCommentsUIState.Success(thread, comments.toList(), commentCount)
                sendThreadUpdated()
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    fun setThreadFollowed(followed: Boolean) {
        viewModelScope.launch {
            try {
                val response = interactor.setThreadFollowed(thread.id, followed)
                thread = thread.copy(following = response.following)
                _uiState.value =
                    DiscussionCommentsUIState.Success(thread, comments.toList(), commentCount)
                sendThreadUpdated()
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    fun setThreadReported(reported: Boolean) {
        viewModelScope.launch {
            try {
                val response = interactor.setThreadFlagged(thread.id, reported)
                thread = thread.copy(abuseFlagged = response.abuseFlagged)
                _uiState.value =
                    DiscussionCommentsUIState.Success(thread, comments.toList(), commentCount)
                sendThreadUpdated()
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    fun setCommentUpvoted(commentId: String, vote: Boolean) {
        viewModelScope.launch {
            try {
                val response = interactor.setCommentVoted(commentId, vote)
                val index = comments.indexOfFirst {
                    it.id == response.id
                }
                comments[index] =
                    comments[index].copy(voted = response.voted, voteCount = response.voteCount)
                _uiState.value =
                    DiscussionCommentsUIState.Success(thread, comments.toList(), commentCount)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    fun setCommentReported(commentId: String, vote: Boolean) {
        viewModelScope.launch {
            try {
                val response = interactor.setCommentFlagged(commentId, vote)
                val index = comments.indexOfFirst {
                    it.id == response.id
                }
                comments[index] = comments[index].copy(abuseFlagged = response.abuseFlagged)
                _uiState.value =
                    DiscussionCommentsUIState.Success(thread, comments.toList(), commentCount)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }

    fun createComment(rawBody: String) {
        viewModelScope.launch {
            try {
                val response = interactor.createComment(thread.id, rawBody, null)
                thread = thread.copy(commentCount = thread.commentCount + 1)
                sendThreadUpdated()
                if (page == -1) {
                    comments.add(response)
                } else {
                    _uiMessage.value =
                        UIMessage.ToastMessage(
                            resourceManager.getString(org.openedx.discussion.R.string.discussion_comment_added)
                        )
                }
                _uiState.value =
                    DiscussionCommentsUIState.Success(thread, comments.toList(), commentCount)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
        }
    }
}
