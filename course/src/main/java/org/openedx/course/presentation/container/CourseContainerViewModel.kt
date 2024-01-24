package org.openedx.course.presentation.container

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.config.Config
import org.openedx.core.exception.NoCachedDataException
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseStructureUpdated
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import java.util.Date

class CourseContainerViewModel(
    val courseId: String,
    var courseName: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val resourceManager: ResourceManager,
    private val notifier: CourseNotifier,
    private val networkConnection: NetworkConnection,
    private val analytics: CourseAnalytics
) : BaseViewModel() {

    val isCourseTopTabBarEnabled get() = config.isCourseTopTabBarEnabled()

    private val _dataReady = MutableLiveData<Boolean?>()
    val dataReady: LiveData<Boolean?>
        get() = _dataReady

    private val _errorMessage = SingleEventLiveData<String>()
    val errorMessage: LiveData<String>
        get() = _errorMessage

    private val _showProgress = MutableLiveData<Boolean>()
    val showProgress: LiveData<Boolean>
        get() = _showProgress

    init {
        viewModelScope.launch {
            notifier.notifier.collect { event ->
                if (event is CourseCompletionSet) {
                    updateData(false)
                }
            }
        }
    }

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
                val courseStructure = interactor.getCourseStructureFromCache()
                courseName = courseStructure.name
                _dataReady.value = courseStructure.start?.let { start ->
                    start < Date()
                }
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

    fun courseTabClickedEvent() {
        analytics.courseTabClickedEvent(courseId, courseName)
    }

    fun videoTabClickedEvent() {
        analytics.videoTabClickedEvent(courseId, courseName)
    }

    fun discussionTabClickedEvent() {
        analytics.discussionTabClickedEvent(courseId, courseName)
    }

    fun datesTabClickedEvent() {
        analytics.datesTabClickedEvent(courseId, courseName)
    }

    fun handoutsTabClickedEvent() {
        analytics.handoutsTabClickedEvent(courseId, courseName)
    }

}