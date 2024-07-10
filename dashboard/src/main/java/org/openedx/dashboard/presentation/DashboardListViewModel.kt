package org.openedx.dashboard.presentation

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.SingleEventLiveData
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.extension.isInternetError
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.IAPAnalyticsEvent
import org.openedx.core.presentation.IAPAnalyticsKeys
import org.openedx.core.presentation.IAPAnalyticsScreen
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.iap.IAPAction
import org.openedx.core.presentation.iap.IAPFlow
import org.openedx.core.presentation.iap.IAPRequestType
import org.openedx.core.presentation.iap.IAPUIState
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseDashboardUpdate
import org.openedx.core.system.notifier.CourseDataUpdated
import org.openedx.core.system.notifier.DiscoveryNotifier
import org.openedx.core.system.notifier.IAPNotifier
import org.openedx.core.system.notifier.UpdateCourseData
import org.openedx.core.system.notifier.app.AppNotifier
import org.openedx.core.system.notifier.app.AppUpgradeEvent
import org.openedx.core.utils.EmailUtil
import org.openedx.dashboard.domain.interactor.DashboardInteractor

class DashboardListViewModel(
    private val appData: AppData,
    private val config: Config,
    private val networkConnection: NetworkConnection,
    private val interactor: DashboardInteractor,
    private val resourceManager: ResourceManager,
    private val discoveryNotifier: DiscoveryNotifier,
    private val iapNotifier: IAPNotifier,
    private val analytics: DashboardAnalytics,
    private val appNotifier: AppNotifier,
    private val preferencesManager: CorePreferences,
    private val iapAnalytics: IAPAnalytics,
    private val iapInteractor: IAPInteractor
) : BaseViewModel() {

    private val coursesList = mutableListOf<EnrolledCourse>()
    private var page = 1
    private var isLoading = false

    val apiHostUrl get() = config.getApiHostURL()

    private val _uiState = MutableLiveData<DashboardUIState>(DashboardUIState.Loading)
    val uiState: LiveData<DashboardUIState>
        get() = _uiState

    private val _uiMessage = SingleEventLiveData<UIMessage>()
    val uiMessage: LiveData<UIMessage>
        get() = _uiMessage

    private val _iapUiState = MutableSharedFlow<IAPUIState?>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val iapUiState: SharedFlow<IAPUIState?>
        get() = _iapUiState.asSharedFlow()

    private val _updating = MutableLiveData<Boolean>()
    val updating: LiveData<Boolean>
        get() = _updating

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private val _canLoadMore = MutableLiveData<Boolean>()
    val canLoadMore: LiveData<Boolean>
        get() = _canLoadMore

    private val _appUpgradeEvent = MutableLiveData<AppUpgradeEvent>()
    val appUpgradeEvent: LiveData<AppUpgradeEvent>
        get() = _appUpgradeEvent

    private val iapConfig
        get() = preferencesManager.appConfig.iapConfig
    private val isIAPEnabled
        get() = iapConfig.isEnabled &&
                iapConfig.disableVersions.contains(appData.versionName).not()

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            discoveryNotifier.notifier.collect {
                if (it is CourseDashboardUpdate) {
                    updateCourses()
                }
            }
        }

        iapNotifier.notifier.onEach { event ->
            when (event) {
                is UpdateCourseData -> {
                    updateCourses(true)
                }
            }
        }.distinctUntilChanged().launchIn(viewModelScope)
    }

    init {
        getCourses()
        collectAppUpgradeEvent()
    }

    fun getCourses() {
        _uiState.value = DashboardUIState.Loading
        coursesList.clear()
        internalLoadingCourses()
    }

    fun updateCourses(isIAPFlow: Boolean = false) {
        if (isLoading) {
            return
        }
        viewModelScope.launch {
            try {
                _updating.value = true
                isLoading = true
                page = 1
                val response = interactor.getEnrolledCourses(page)
                if (response.pagination.next.isNotEmpty() && page != response.pagination.numPages) {
                    _canLoadMore.value = true
                    page++
                } else {
                    _canLoadMore.value = false
                    page = -1
                }
                coursesList.clear()
                coursesList.addAll(response.courses)
                if (coursesList.isEmpty()) {
                    _uiState.value = DashboardUIState.Empty
                } else {
                    _uiState.value = DashboardUIState.Courses(
                        courses = ArrayList(coursesList),
                        isValuePropEnabled = preferencesManager.appConfig.isValuePropEnabled
                    )
                }
                if (isIAPFlow) {
                    iapNotifier.send(CourseDataUpdated())
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
            isLoading = false
        }
    }

    private fun internalLoadingCourses() {
        viewModelScope.launch {
            try {
                isLoading = true
                val response = if (networkConnection.isOnline() || page > 1) {
                    interactor.getEnrolledCourses(page)
                } else {
                    null
                }
                if (response != null) {
                    if (response.pagination.next.isNotEmpty() && page != response.pagination.numPages) {
                        _canLoadMore.value = true
                        page++
                    } else {
                        _canLoadMore.value = false
                        page = -1
                    }
                    coursesList.addAll(response.courses)
                } else {
                    val cachedList = interactor.getEnrolledCoursesFromCache()
                    _canLoadMore.value = false
                    page = -1
                    coursesList.addAll(cachedList)
                }
                if (coursesList.isEmpty()) {
                    _uiState.value = DashboardUIState.Empty
                } else {
                    _uiState.value = DashboardUIState.Courses(
                        courses = ArrayList(coursesList),
                        isValuePropEnabled = preferencesManager.appConfig.isValuePropEnabled
                    )
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
            isLoading = false
        }
    }

    fun fetchMore() {
        if (!isLoading && page != -1) {
            internalLoadingCourses()
        }
    }

    private fun collectAppUpgradeEvent() {
        viewModelScope.launch {
            appNotifier.notifier.collect { event ->
                if (event is AppUpgradeEvent) {
                    _appUpgradeEvent.value = event
                }
            }
        }
    }

    fun dashboardCourseClickedEvent(courseId: String, courseName: String) {
        analytics.dashboardCourseClickedEvent(courseId, courseName)
    }

    fun detectUnfulfilledPurchase() {
        if (isIAPEnabled) {
            viewModelScope.launch(Dispatchers.IO) {
                preferencesManager.user?.id?.let { userId ->
                    runCatching {
                        iapInteractor.processUnfulfilledPurchase(userId)
                    }.onSuccess {
                        if (it) {
                            unfulfilledPurchaseInitiatedEvent()
                            _iapUiState.emit(IAPUIState.PurchasesFulfillmentCompleted)
                        }
                    }.onFailure {
                        if (it is IAPException) {
                            _iapUiState.emit(
                                IAPUIState.Error(
                                    IAPException(
                                        IAPRequestType.UNFULFILLED_CODE,
                                        it.httpErrorCode,
                                        it.errorMessage
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    private fun unfulfilledPurchaseInitiatedEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_UNFULFILLED_PURCHASE_INITIATED)
    }

    fun showFeedbackScreen(context: Context, message: String) {
        EmailUtil.showFeedbackScreen(
            context = context,
            feedbackEmailAddress = config.getFeedbackEmailAddress(),
            subject = context.getString(R.string.core_error_upgrading_course_in_app),
            feedback = message,
            appVersion = appData.versionName
        )
        logIAPEvent(IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION, buildMap {
            put(IAPAnalyticsKeys.ERROR_ALERT_TYPE.key, IAPAction.ACTION_UNFULFILLED.action)
            put(IAPAnalyticsKeys.ERROR_ACTION.key, IAPAction.ACTION_GET_HELP.action)
        }.toMutableMap())
    }

    fun logIAPCancelEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION, buildMap {
            put(IAPAnalyticsKeys.ERROR_ALERT_TYPE.key, IAPAction.ACTION_UNFULFILLED.action)
            put(IAPAnalyticsKeys.ERROR_ACTION.key, IAPAction.ACTION_CLOSE.action)
        }.toMutableMap())
    }

    private fun logIAPEvent(
        event: IAPAnalyticsEvent,
        params: MutableMap<String, Any?> = mutableMapOf()
    ) {
        iapAnalytics.logEvent(event.eventName, params.apply {
            put(IAPAnalyticsKeys.NAME.key, event.biValue)
            put(IAPAnalyticsKeys.SCREEN_NAME.key, IAPAnalyticsScreen.COURSE_ENROLLMENT.screenName)
            put(IAPAnalyticsKeys.IAP_FLOW_TYPE.key, IAPFlow.SILENT.value)
            put(IAPAnalyticsKeys.CATEGORY.key, IAPAnalyticsKeys.IN_APP_PURCHASES.key)
        })
    }

    fun clearIAPState() {
        viewModelScope.launch {
            _iapUiState.emit(null)
        }
    }
}
