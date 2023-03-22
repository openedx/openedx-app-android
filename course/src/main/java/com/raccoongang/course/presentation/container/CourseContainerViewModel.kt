package com.raccoongang.course.presentation.container

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.system.connection.NetworkConnection
import com.raccoongang.core.R
import com.raccoongang.core.SingleEventLiveData
import com.raccoongang.core.exception.NoCachedDataException
import com.raccoongang.core.extension.isInternetError
import com.raccoongang.core.system.ResourceManager
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.core.system.notifier.CourseStructureUpdated
import com.raccoongang.course.domain.interactor.CourseInteractor
import kotlinx.coroutines.launch

class CourseContainerViewModel(
    val courseId: String,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: CourseNotifier,
    private val networkConnection: NetworkConnection
) : BaseViewModel() {

    private val _dataReady = MutableLiveData<Boolean>()
    val dataReady: LiveData<Boolean>
        get() = _dataReady

    private val _errorMessage = SingleEventLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _showProgress = MutableLiveData<Boolean>()
    val showProgress: LiveData<Boolean>
        get() = _showProgress

    fun preloadCourseStructure() {
        if (_dataReady.value != null) {
            return
        }

        _showProgress.value = true
        viewModelScope.launch {
            try {
                if (networkConnection.isOnline()) {
                    interactor.preloadCourseStructure(courseId)
                } else {
                    interactor.preloadCourseStructureFromCache(courseId)
                }
                _dataReady.value = true
            } catch (e: Exception) {
                if (e.isInternetError() || e is NoCachedDataException) {
                    _errorMessage.value =
                        resourceManager.getString(R.string.core_error_no_connection)
                } else {
                    _errorMessage.value =
                        resourceManager.getString(R.string.core_error_unknown_error)
                }
            }
            _showProgress.value = false
        }
    }

    fun updateData(withSwipeRefresh: Boolean) {
        _showProgress.value = true
        viewModelScope.launch {
            try {
                interactor.preloadCourseStructure(courseId)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _errorMessage.value =
                        resourceManager.getString(R.string.core_error_no_connection)
                } else {
                    _errorMessage.value =
                        resourceManager.getString(R.string.core_error_unknown_error)
                }
            }
            _showProgress.value = false
            notifier.send(CourseStructureUpdated(courseId, withSwipeRefresh))
        }
    }


}