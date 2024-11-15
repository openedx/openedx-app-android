package org.openedx.course.presentation.unit.html

import android.content.res.AssetManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.config.Config
import org.openedx.core.presentation.global.ErrorType
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.worker.OfflineProgressSyncScheduler
import org.openedx.foundation.extension.readAsText
import org.openedx.foundation.presentation.BaseViewModel

class HtmlUnitViewModel(
    private val blockId: String,
    private val courseId: String,
    private val config: Config,
    private val edxCookieManager: AppCookieManager,
    private val networkConnection: NetworkConnection,
    private val notifier: CourseNotifier,
    private val courseInteractor: CourseInteractor,
    private val offlineProgressSyncScheduler: OfflineProgressSyncScheduler
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<HtmlUnitUIState>(HtmlUnitUIState.Initialization)
    val uiState = _uiState.asStateFlow()

    private val _injectJSList = MutableStateFlow<List<String>>(listOf())
    val injectJSList = _injectJSList.asStateFlow()

    val isOnline get() = networkConnection.isOnline()
    val isCourseUnitProgressEnabled get() = config.getCourseUIConfig().isCourseUnitProgressEnabled
    val apiHostURL get() = config.getApiHostURL()
    val cookieManager get() = edxCookieManager

    init {
        tryToSyncProgress()
    }

    fun onWebPageLoading() {
        _uiState.value = HtmlUnitUIState.Loading
    }

    fun onWebPageLoaded() {
        _uiState.value = HtmlUnitUIState.Loaded()
    }

    fun onWebPageLoadError() {
        _uiState.value = HtmlUnitUIState.Error(
            if (networkConnection.isOnline()) {
                ErrorType.UNKNOWN_ERROR
            } else {
                ErrorType.CONNECTION_ERROR
            }
        )
    }

    fun setWebPageLoaded(assets: AssetManager) {
        if (_injectJSList.value.isNotEmpty()) return

        val jsList = mutableListOf<String>()

        // Injection to intercept completion state for xBlocks
        assets.readAsText("js_injection/completions.js")?.let { jsList.add(it) }
        // Injection to fix CSS issues for Survey xBlock
        assets.readAsText("js_injection/survey_css.js")?.let { jsList.add(it) }

        _injectJSList.value = jsList
        getXBlockProgress()
    }

    fun notifyCompletionSet() {
        viewModelScope.launch {
            notifier.send(CourseCompletionSet())
        }
    }

    fun saveXBlockProgress(jsonProgress: String) {
        viewModelScope.launch {
            courseInteractor.saveXBlockProgress(blockId, courseId, jsonProgress)
            offlineProgressSyncScheduler.scheduleSync()
        }
    }

    private fun tryToSyncProgress() {
        viewModelScope.launch {
            try {
                if (isOnline) {
                    courseInteractor.submitOfflineXBlockProgress(blockId, courseId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.value = HtmlUnitUIState.Loading
            }
        }
    }

    private fun getXBlockProgress() {
        viewModelScope.launch {
            if (!isOnline) {
                val xBlockProgress = courseInteractor.getXBlockProgress(blockId)
                delay(500)
                _uiState.value = HtmlUnitUIState.Loaded(jsonProgress = xBlockProgress?.jsonProgress?.toJson())
            }
        }
    }
}
