package org.openedx.course.presentation.dates

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Block
import org.openedx.core.extension.getSequentialBlocks
import org.openedx.core.extension.getVerticalBlocks
import org.openedx.core.extension.isInternetError
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.course.domain.interactor.CourseInteractor

class CourseDatesViewModel(
    val courseId: String,
    val isSelfPaced: Boolean,
    private val interactor: CourseInteractor,
    private val networkConnection: NetworkConnection,
    private val resourceManager: ResourceManager,
) : BaseViewModel() {

    private val _uiState = MutableLiveData<DatesUIState>(DatesUIState.Loading)
    val uiState: LiveData<DatesUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _updating = MutableLiveData<Boolean>()
    val updating: LiveData<Boolean>
        get() = _updating

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    init {
        getCourseDates()
    }

    fun getCourseDates(swipeToRefresh: Boolean = false) {
        if (!swipeToRefresh) {
            _uiState.value = DatesUIState.Loading
        }
        _updating.value = swipeToRefresh
        loadingCourseDatesInternal()
    }

    private fun loadingCourseDatesInternal() {
        viewModelScope.launch {
            try {
                val datesResponse = interactor.getCourseDates(courseId = courseId)
                if (datesResponse.datesSection.isEmpty()) {
                    _uiState.value = DatesUIState.Empty
                } else {
                    _uiState.value = DatesUIState.Dates(datesResponse)
                }
            } catch (e: Exception) {
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

    fun resetCourseDatesBanner(onResetDates: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                interactor.resetCourseDates(courseId = courseId)
                getCourseDates()
                onResetDates(true)
            } catch (e: Exception) {
                if (e.isInternetError()) {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_no_connection))
                } else {
                    _uiMessage.value =
                        UIMessage.SnackBarMessage(resourceManager.getString(R.string.core_error_unknown_error))
                }
                onResetDates(false)
            }
        }
    }

    fun getVerticalBlock(blockId: String): Block? {
        return try {
            val courseStructure = interactor.getCourseStructureFromCache()
            courseStructure.blockData.getVerticalBlocks().find { it.descendants.contains(blockId) }
        } catch (e: Exception) {
            null
        }
    }

    fun getSequentialBlock(blockId: String): Block? {
        return try {
            val courseStructure = interactor.getCourseStructureFromCache()
            courseStructure.blockData.getSequentialBlocks()
                .find { it.descendants.contains(blockId) }
        } catch (e: Exception) {
            null
        }
    }
}
