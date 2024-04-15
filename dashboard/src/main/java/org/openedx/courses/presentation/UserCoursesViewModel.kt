package org.openedx.courses.presentation

import androidx.lifecycle.LifecycleOwner
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
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.dashboard.domain.interactor.DashboardInteractor

class UserCoursesViewModel(
    private val config: Config,
    private val interactor: DashboardInteractor,
    private val resourceManager: ResourceManager,
    private val discoveryNotifier: DiscoveryNotifier,
) : BaseViewModel() {

    val apiHostUrl get() = config.getApiHostURL()
    val isProgramTypeWebView get() = config.getProgramConfig().isViewTypeWebView()

    private val _uiState = MutableLiveData<UserCoursesUIState>(UserCoursesUIState.Loading)
    val uiState: LiveData<UserCoursesUIState>
        get() = _uiState

    private val _uiMessage = MutableStateFlow<UIMessage?>(null)
    val uiMessage: SharedFlow<UIMessage?>
        get() = _uiMessage.asStateFlow()

    private val _updating = MutableLiveData<Boolean>()
    val updating: LiveData<Boolean>
        get() = _updating

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            discoveryNotifier.notifier.collect {
                // TODO Notifier doesn't collect data
                if (it is CourseDashboardUpdate) {
                    updateCourses()
                }
            }
        }
    }

    init {
        getCourses()
    }

    private fun getCourses() {
        viewModelScope.launch {
            try {
                val response = interactor.getUserCourses()
                if (response.primary == null) {
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

    fun updateCourses() {
        _updating.value = true
        getCourses()
    }
}
