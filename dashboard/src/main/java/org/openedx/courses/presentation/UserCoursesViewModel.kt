package org.openedx.courses.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.dashboard.domain.interactor.DashboardInteractor

class UserCoursesViewModel(
    private val config: Config,
    private val interactor: DashboardInteractor,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    val apiHostUrl get() = config.getApiHostURL()

    private val _uiState = MutableLiveData<UserCoursesUIState>(UserCoursesUIState.Loading)
    val uiState: LiveData<UserCoursesUIState>
        get() = _uiState

    private val _uiMessage = MutableStateFlow<UIMessage?>(null)
    val uiMessage: SharedFlow<UIMessage?>
        get() = _uiMessage.asStateFlow()

    private val _updating = MutableLiveData<Boolean>()
    val updating: LiveData<Boolean>
        get() = _updating

    init {
        getCourses()
    }

    private fun getCourses() {
        viewModelScope.launch {
            try {
                val response = interactor.getUserCourses()
                if (response.enrollments.courses.isEmpty()) {
                    _uiState.value = UserCoursesUIState.Empty
                } else {
                    _uiState.value = UserCoursesUIState.Courses(response)
                }
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection)))
                } else {
                    _uiMessage.emit(UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error)))
                }
            } finally {
                _updating.value = false
            }
        }
    }

    fun updateCoursed() {
        _updating.value = true
        getCourses()
    }
}
