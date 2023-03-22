package com.raccoongang.course.presentation.progress

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.R
import com.raccoongang.core.SingleEventLiveData
import com.raccoongang.core.UIMessage
import com.raccoongang.core.extension.isInternetError
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.course.domain.interactor.CourseInteractor
import com.raccoongang.course.presentation.outline.CourseOutlineUIState
import kotlinx.coroutines.launch

class CourseProgressViewModel(
    private val courseId: String,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    var courseTitle = ""
    var courseImage = ""

    private val _uiState = MutableLiveData<CourseProgressUIState>(CourseProgressUIState.Loading)
    val uiState: LiveData<CourseProgressUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    fun getProgress() {
        viewModelScope.launch {
            try {
                val progress = interactor.getProgress(courseId)
                _uiState.value = CourseProgressUIState.Data(progress.sections, progress.progress)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value = UIMessage.SnackBarMessage(
                        resourceManager.getString(R.string.core_error_no_connection)
                    )
                } else {
                    _uiMessage.value = UIMessage.SnackBarMessage(
                        resourceManager.getString(R.string.core_error_unknown_error)
                    )
                }
            }
        }
    }
}
