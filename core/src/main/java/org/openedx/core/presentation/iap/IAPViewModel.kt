package org.openedx.core.presentation.iap

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.module.billing.getCourseSku
import org.openedx.core.module.billing.getPriceAmount
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.IAPAnalyticsEvent
import org.openedx.core.presentation.IAPAnalyticsKeys
import org.openedx.core.presentation.global.AppData
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.notifier.CourseDataUpdated
import org.openedx.core.system.notifier.IAPNotifier
import org.openedx.core.system.notifier.UpdateCourseData
import org.openedx.core.utils.EmailUtil
import org.openedx.core.utils.TimeUtils

class IAPViewModel(
    iapFlow: IAPFlow,
    private val purchaseFlowData: PurchaseFlowData,
    private val appData: AppData,
    private val iapInteractor: IAPInteractor,
    private val corePreferences: CorePreferences,
    private val analytics: IAPAnalytics,
    private val resourceManager: ResourceManager,
    private val config: Config,
    private val iapNotifier: IAPNotifier
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<IAPUIState>(IAPUIState.Loading(IAPLoaderType.PRICE))
    val uiState: StateFlow<IAPUIState>
        get() = _uiState.asStateFlow()

    private val _uiMessage = MutableSharedFlow<UIMessage>()
    val uiMessage: SharedFlow<UIMessage>
        get() = _uiMessage.asSharedFlow()

    val purchaseData: PurchaseFlowData
        get() = purchaseFlowData

    private val purchaseListeners = object : BillingProcessor.PurchaseListeners {
        override fun onPurchaseComplete(purchase: Purchase) {
            if (purchase.getCourseSku() == purchaseFlowData.productInfo?.courseSku) {
                _uiState.value =
                    IAPUIState.Loading(loaderType = IAPLoaderType.FULL_SCREEN)
                purchaseFlowData.purchaseToken = purchase.purchaseToken
                executeOrder(purchaseFlowData)
            }
        }

        override fun onPurchaseCancel(responseCode: Int, message: String) {
            updateErrorState(
                IAPException(
                    IAPRequestType.PAYMENT_SDK_CODE,
                    httpErrorCode = responseCode,
                    errorMessage = message
                )
            )
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            iapNotifier.notifier.onEach { event ->
                when (event) {
                    is CourseDataUpdated -> {
                        upgradeSuccessEvent()
                        _uiMessage.emit(UIMessage.ToastMessage(resourceManager.getString(R.string.iap_success_message)))
                        _uiState.value = IAPUIState.CourseDataUpdated
                    }
                }
            }.distinctUntilChanged().launchIn(viewModelScope)
        }

        when (iapFlow) {
            IAPFlow.USER_INITIATED -> {
                loadPrice()
            }

            IAPFlow.SILENT, IAPFlow.RESTORE -> {
                _uiState.value = IAPUIState.Loading(IAPLoaderType.FULL_SCREEN)
                purchaseFlowData.flowStartTime = TimeUtils.getCurrentTime()
                updateCourseData()
            }
        }
    }

    fun loadPrice() {
        viewModelScope.launch(Dispatchers.IO) {
            purchaseFlowData.takeIf { it.courseId != null && it.productInfo != null }
                ?.apply {
                    _uiState.value = IAPUIState.Loading(loaderType = IAPLoaderType.PRICE)
                    runCatching {
                        iapInteractor.loadPrice(purchaseFlowData.productInfo?.storeSku!!)
                    }.onSuccess {
                        this.formattedPrice = it.formattedPrice
                        this.price = it.getPriceAmount()
                        this.currencyCode = it.priceCurrencyCode
                        _uiState.value =
                            IAPUIState.ProductData(formattedPrice = this.formattedPrice!!)
                    }.onFailure {
                        if (it is IAPException) {
                            updateErrorState(it)
                        }
                    }
                } ?: run {
                updateErrorState(
                    IAPException(
                        requestType = IAPRequestType.PRICE_CODE,
                        httpErrorCode = IAPRequestType.PRICE_CODE.hashCode(),
                        errorMessage = "Product SKU is not provided in the request."
                    )
                )
            }
        }
    }

    fun startPurchaseFlow() {
        upgradeNowClickedEvent()
        _uiState.value = IAPUIState.Loading(loaderType = IAPLoaderType.PURCHASE_FLOW)
        purchaseFlowData.flowStartTime = TimeUtils.getCurrentTime()
        purchaseFlowData.takeIf { purchaseFlowData.courseName != null && it.productInfo != null }
            ?.apply {
                addToBasket(productInfo?.courseSku!!)
            } ?: run {
            updateErrorState(
                IAPException(
                    requestType = IAPRequestType.NO_SKU_CODE,
                    httpErrorCode = IAPRequestType.NO_SKU_CODE.hashCode(),
                    errorMessage = ""
                )
            )
        }
    }

    private fun addToBasket(courseSku: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                iapInteractor.addToBasket(courseSku)
            }.onSuccess { basketId ->
                purchaseFlowData.basketId = basketId
                processCheckout(basketId)
            }.onFailure {
                if (it is IAPException) {
                    updateErrorState(it)
                }
            }
        }
    }

    private fun processCheckout(basketId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                iapInteractor.processCheckout(basketId)
            }.onSuccess {
                _uiState.value = IAPUIState.PurchaseProduct
            }.onFailure {
                if (it is IAPException) {
                    updateErrorState(it)
                }
            }
        }
    }

    fun purchaseItem(activity: FragmentActivity) {
        viewModelScope.launch(Dispatchers.IO) {
            takeIf {
                corePreferences.user?.id != null && purchaseFlowData.productInfo != null
            }?.apply {
                iapInteractor.purchaseItem(
                    activity,
                    corePreferences.user?.id!!,
                    purchaseFlowData.productInfo!!,
                    purchaseListeners
                )
            }
        }
    }

    private fun executeOrder(purchaseFlowData: PurchaseFlowData) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                iapInteractor.executeOrder(
                    basketId = purchaseFlowData.basketId,
                    purchaseToken = purchaseFlowData.purchaseToken!!,
                    price = purchaseFlowData.price,
                    currencyCode = purchaseFlowData.currencyCode,
                )
            }.onSuccess {
                consumeOrderForFurtherPurchases(purchaseFlowData)
            }.onFailure {
                if (it is IAPException) {
                    updateErrorState(it)
                }
            }
        }
    }

    private fun consumeOrderForFurtherPurchases(purchaseFlowData: PurchaseFlowData) {
        viewModelScope.launch(Dispatchers.IO) {
            purchaseFlowData.purchaseToken?.let {
                runCatching {
                    iapInteractor.consumePurchase(it)
                }.onSuccess {
                    updateCourseData()
                }.onFailure {
                    if (it is IAPException) {
                        updateErrorState(it)
                    }
                }
            }
        }
    }

    fun refreshCourse() {
        _uiState.value = IAPUIState.Loading(IAPLoaderType.FULL_SCREEN)
        purchaseFlowData.flowStartTime = TimeUtils.getCurrentTime()
        updateCourseData()
    }

    fun retryExecuteOrder() {
        executeOrder(purchaseFlowData)
    }

    fun retryToConsumeOrder() {
        consumeOrderForFurtherPurchases(purchaseFlowData)
    }

    private fun updateCourseData() {
        viewModelScope.launch(Dispatchers.IO) {
            purchaseFlowData.courseId?.let { courseId ->
                iapNotifier.send(UpdateCourseData(courseId))
            }
        }
    }

    fun showFeedbackScreen(context: Context, flowType: String, message: String) {
        EmailUtil.showFeedbackScreen(
            context = context,
            feedbackEmailAddress = config.getFeedbackEmailAddress(),
            subject = context.getString(R.string.core_error_upgrading_course_in_app),
            feedback = message,
            appVersion = appData.versionName
        )
        logIAPErrorActionEvent(flowType, IAPAction.ACTION_GET_HELP.action)
    }

    private fun updateErrorState(iapException: IAPException) {
        val feedbackErrorMessage: String = iapException.getFormattedErrorMessage()
        when (iapException.requestType) {
            IAPRequestType.PAYMENT_SDK_CODE -> {
                if (BillingClient.BillingResponseCode.USER_CANCELED == iapException.httpErrorCode) {
                    canceledByUserEvent()
                } else {
                    purchaseErrorEvent(feedbackErrorMessage)
                }
            }

            IAPRequestType.PRICE_CODE,
            IAPRequestType.NO_SKU_CODE -> {
                priceLoadErrorEvent(feedbackErrorMessage)
            }

            else -> {
                courseUpgradeErrorEvent(feedbackErrorMessage)
            }
        }
        if (BillingClient.BillingResponseCode.USER_CANCELED != iapException.httpErrorCode) {
            _uiState.value = IAPUIState.Error(iapException)
        } else {
            _uiState.value = IAPUIState.Clear
        }
    }

    private fun upgradeNowClickedEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_UPGRADE_NOW_CLICKED)
    }

    private fun upgradeSuccessEvent() {
        val elapsedTime = TimeUtils.getCurrentTime() - purchaseFlowData.flowStartTime
        logIAPEvent(IAPAnalyticsEvent.IAP_COURSE_UPGRADE_SUCCESS, buildMap {
            put(IAPAnalyticsKeys.ELAPSED_TIME.key, elapsedTime)
        }.toMutableMap())
    }

    private fun purchaseErrorEvent(error: String) {
        logIAPEvent(IAPAnalyticsEvent.IAP_PAYMENT_ERROR, buildMap {
            put(IAPAnalyticsKeys.ERROR.key, error)
        }.toMutableMap())
    }

    private fun canceledByUserEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_PAYMENT_CANCELED)
    }

    private fun courseUpgradeErrorEvent(error: String) {
        logIAPEvent(IAPAnalyticsEvent.IAP_COURSE_UPGRADE_ERROR, buildMap {
            put(IAPAnalyticsKeys.ERROR.key, error)
        }.toMutableMap())
    }

    private fun priceLoadErrorEvent(error: String) {
        logIAPEvent(IAPAnalyticsEvent.IAP_PRICE_LOAD_ERROR, buildMap {
            put(IAPAnalyticsKeys.ERROR.key, error)
        }.toMutableMap())
    }

    fun logIAPErrorActionEvent(alertType: String, action: String) {
        logIAPEvent(IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION, buildMap {
            put(IAPAnalyticsKeys.ERROR_ALERT_TYPE.key, alertType)
            put(IAPAnalyticsKeys.ERROR_ACTION.key, action)
        }.toMutableMap())
    }

    private fun logIAPEvent(
        event: IAPAnalyticsEvent,
        params: MutableMap<String, Any?> = mutableMapOf()
    ) {
        analytics.logEvent(event.eventName, params.apply {
            put(IAPAnalyticsKeys.NAME.key, event.biValue)
            purchaseFlowData.takeIf { it.courseId.isNullOrBlank().not() }?.let {
                put(IAPAnalyticsKeys.COURSE_ID.key, purchaseFlowData.courseId)
                put(
                    IAPAnalyticsKeys.PACING.key,
                    if (purchaseFlowData.isSelfPaced == true) IAPAnalyticsKeys.SELF.key else IAPAnalyticsKeys.INSTRUCTOR.key
                )
            }
            purchaseFlowData.formattedPrice?.takeIf { it.isNotBlank() }?.let { formattedPrice ->
                put(IAPAnalyticsKeys.PRICE.key, formattedPrice)
            }
            purchaseFlowData.componentId?.takeIf { it.isNotBlank() }?.let { componentId ->
                put(IAPAnalyticsKeys.COMPONENT_ID.key, componentId)
            }
            put(IAPAnalyticsKeys.SCREEN_NAME.key, purchaseFlowData.screenName)
            put(IAPAnalyticsKeys.CATEGORY.key, IAPAnalyticsKeys.IN_APP_PURCHASES.key)
        })
    }

    fun clearIAPFLow() {
        _uiState.value = IAPUIState.Clear
        purchaseFlowData.reset()
    }
}
