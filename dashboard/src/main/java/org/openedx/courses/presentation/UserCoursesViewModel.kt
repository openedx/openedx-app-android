package org.openedx.courses.presentation

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.utils.FileUtil
import org.openedx.courses.domain.model.UserCourses
import org.openedx.dashboard.domain.interactor.DashboardInteractor

class UserCoursesViewModel(
    private val config: Config,
    private val interactor: DashboardInteractor,
    private val resourceManager: ResourceManager,
    private val discoveryNotifier: DiscoveryNotifier,
    private val networkConnection: NetworkConnection,
    private val fileUtil: FileUtil
) : BaseViewModel() {

    val apiHostUrl get() = config.getApiHostURL()
    val isProgramTypeWebView get() = config.getProgramConfig().isViewTypeWebView()

    private val _uiState = MutableStateFlow<UserCoursesUIState>(UserCoursesUIState.Loading)
    val uiState: StateFlow<UserCoursesUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage?>
        get() = _uiMessage.asSharedFlow()

    private val _updating = MutableStateFlow<Boolean>(false)
    val updating: StateFlow<Boolean>
        get() = _updating.asStateFlow()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

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

    fun getCourses() {
        viewModelScope.launch {
            try {
                if (networkConnection.isOnline()) {
                    val response = interactor.getMainUserCourses()
                    if (response.primary == null && response.enrollments.isNotEmpty()) {
                        _uiState.value = UserCoursesUIState.Empty
                    } else {
                        _uiState.value = UserCoursesUIState.Courses(response)
                    }
                } else {
                    val cachedUserCourses = fileUtil.getObjectFromFile<UserCourses>()
                    if (cachedUserCourses == null) {
                        _uiState.value = UserCoursesUIState.Empty
                    } else {
                        _uiState.value = UserCoursesUIState.Courses(cachedUserCourses)
                    }
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
