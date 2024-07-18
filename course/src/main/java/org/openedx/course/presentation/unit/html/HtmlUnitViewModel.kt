package org.openedx.course.presentation.unit.html

import android.content.res.AssetManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.core.extension.readAsText
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.global.ErrorType
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseNotifier

class HtmlUnitViewModel(
    private val config: Config,
    private val appData: AppData,
    private val edxCookieManager: AppCookieManager,
    private val networkConnection: NetworkConnection,
    private val notifier: CourseNotifier
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<HtmlUnitUIState>(HtmlUnitUIState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _injectJSList = MutableStateFlow<List<String>>(listOf())
    val injectJSList = _injectJSList.asStateFlow()

    val isOnline get() = networkConnection.isOnline()
    val isCourseUnitProgressEnabled get() = config.getCourseUIConfig().isCourseUnitProgressEnabled
    val apiHostURL get() = config.getApiHostURL()
    val cookieManager get() = edxCookieManager
    val appUserAgent get() = appData.appUserAgent

    fun onWebPageLoading() {
        _uiState.value = HtmlUnitUIState.Loading
    }

    fun onWebPageLoaded() {
        _uiState.value = HtmlUnitUIState.Loaded
    }

    fun onWebPageLoadError() {
        _uiState.value =
            HtmlUnitUIState.Error(if (networkConnection.isOnline()) ErrorType.UNKNOWN_ERROR else ErrorType.CONNECTION_ERROR)
    }

    fun setWebPageLoaded(assets: AssetManager) {
        if (_injectJSList.value.isNotEmpty()) return

        val jsList = mutableListOf<String>()

        //Injection to intercept completion state for xBlocks
        assets.readAsText("js_injection/completions.js")?.let { jsList.add(it) }
        //Injection to fix CSS issues for Survey xBlock
        assets.readAsText("js_injection/survey_css.js")?.let { jsList.add(it) }

        _injectJSList.value = jsList
    }

    fun notifyCompletionSet() {
        viewModelScope.launch {
            notifier.send(CourseCompletionSet())
        }
    }
}
