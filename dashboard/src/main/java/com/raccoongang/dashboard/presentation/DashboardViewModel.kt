package com.raccoongang.dashboard.presentation

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.R
import com.raccoongang.core.SingleEventLiveData
import com.raccoongang.core.UIMessage
import com.raccoongang.core.domain.model.EnrolledCourse
import com.raccoongang.core.extension.isInternetError
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.system.notifier.CourseDashboardUpdate
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.dashboard.domain.interactor.DashboardInteractor
import kotlinx.coroutines.launch


class DashboardViewModel(
    private val networkConnection: NetworkConnection,
    private val interactor: DashboardInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: CourseNotifier
) : BaseViewModel() {

    private val coursesList = mutableListOf<EnrolledCourse>()
    private val _uiState = MutableLiveData<DashboardUIState>(DashboardUIState.Loading)
    val uiState: LiveData<DashboardUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _updating = MutableLiveData<Boolean>()
    val updating: LiveData<Boolean>
        get() = _updating

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect {
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
        _uiState.value = DashboardUIState.Loading
        coursesList.clear()
        internalLoadingCourses()
    }

    fun updateCourses() {
        _updating.value = true
        viewModelScope.launch {
            try {
                val response = interactor.getEnrolledCourses()
                coursesList.clear()
                coursesList.addAll(response.toList())
                if (coursesList.isEmpty()) {
                    _uiState.value = DashboardUIState.Empty
                } else {
                    _uiState.value = DashboardUIState.Courses(coursesList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
            }
            _updating.value = false
        }
    }

    private fun internalLoadingCourses() {
        viewModelScope.launch {
            try {
                val response = if (networkConnection.isOnline()) {
                    interactor.getEnrolledCourses()
                } else {
                    interactor.getEnrolledCoursesFromCache()
                }
                coursesList.addAll(response.toList())
                if (coursesList.isEmpty()) {
                    _uiState.value = DashboardUIState.Empty
                } else {
                    _uiState.value = DashboardUIState.Courses(coursesList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
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