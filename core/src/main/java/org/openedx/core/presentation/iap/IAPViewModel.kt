package org.openedx.core.presentation.iap

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.interactor.IAPInteractor
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPErrorMessage
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.module.billing.getCourseSku
import org.openedx.core.module.billing.getPriceAmount
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.IAPAnalyticsEvent
import org.openedx.core.presentation.IAPAnalyticsKeys
import org.openedx.core.system.notifier.CourseDataUpdated
import org.openedx.core.system.notifier.IAPNotifier
import org.openedx.core.system.notifier.UpdateCourseData
import org.openedx.core.utils.EmailUtil
import java.util.Calendar

class IAPViewModel(
    iapAction: IAPAction,
    var purchaseFlowData: PurchaseFlowData,
    private val versionName: String,
    private val iapInteractor: IAPInteractor,
    private val corePreferences: CorePreferences,
    private val analytics: IAPAnalytics,
    private val config: Config,
    private val iapNotifier: IAPNotifier
) : BaseViewModel() {

    private val _uiState = MutableStateFlow<IAPUIState>(IAPUIState.Clear)
    val uiState: StateFlow<IAPUIState>
        get() = _uiState.asStateFlow()

    private val purchaseListeners = object : BillingProcessor.PurchaseListeners {
        override fun onPurchaseComplete(purchase: Purchase) {
            if (purchase.getCourseSku() == purchaseFlowData.productInfo?.courseSku) {
                _uiState.value =
                    IAPUIState.Loading(
                        courseName = purchaseFlowData.courseName!!,
                        loaderType = IAPLoaderType.FULL_SCREEN
                    )
                purchaseFlowData.purchaseToken = purchase.purchaseToken
                executeOrder(purchaseFlowData)
            }
        }

        override fun onPurchaseCancel(responseCode: Int, message: String) {
            updateErrorState(
                requestType = IAPErrorMessage.PAYMENT_SDK_CODE,
                responseCode = responseCode,
                errorMessage = message
            )
        }
    }

    init {
        viewModelScope.launch {
            iapNotifier.notifier.collect { event ->
                when (event) {
                    is CourseDataUpdated -> {
                        _uiState.value = IAPUIState.CourseDataUpdated
                    }
                }
            }
        }

        when (iapAction) {
            IAPAction.LOAD_PRICE -> {
                loadPrice()
            }

            else -> {}
        }
    }

    private fun updateErrorState(requestType: Int, responseCode: Int, errorMessage: String) {
        val feedbackErrorMessage: String =
            IAPErrorMessage.getFormattedErrorMessage(requestType, responseCode, errorMessage)
                .toString()
        _uiState.value = IAPUIState.Error(
            courseName = purchaseFlowData.courseName!!,
            requestType = requestType,
            feedbackErrorMessage = feedbackErrorMessage
        )
        when (requestType) {
            IAPErrorMessage.PAYMENT_SDK_CODE -> {
                if (BillingClient.BillingResponseCode.USER_CANCELED == responseCode) {
                    canceledByUserEvent()
                } else {
                    purchaseErrorEvent(feedbackErrorMessage)
                }
            }

            IAPErrorMessage.PRICE_CODE,
            IAPErrorMessage.NO_SKU_CODE -> {
                priceLoadErrorEvent(feedbackErrorMessage)
            }

            else -> {
                courseUpgradeErrorEvent(feedbackErrorMessage)
            }
        }
    }

    private fun loadPrice() {
        viewModelScope.launch {
            purchaseFlowData.takeIf { it.courseId != null && it.productInfo != null }
                ?.apply {
                    _uiState.value = IAPUIState.Loading(
                        courseName = this.courseName!!,
                        loaderType = IAPLoaderType.PRICE
                    )
                    runCatching {
                        iapInteractor.loadPrice(purchaseFlowData.productInfo?.storeSku!!)
                    }.onSuccess {
                        this.formattedPrice = it.formattedPrice
                        this.price = it.getPriceAmount()
                        this.currencyCode = it.priceCurrencyCode
                        _uiState.value = IAPUIState.ProductData(
                            courseName = this.courseName,
                            formattedPrice = this.formattedPrice!!
                        )
                    }.onFailure {
                        if (it is IAPException) {
                            updateErrorState(
                                requestType = IAPErrorMessage.PRICE_CODE,
                                responseCode = it.httpErrorCode,
                                errorMessage = it.errorMessage,
                            )
                        }
                    }
                } ?: run {
                updateErrorState(
                    requestType = IAPErrorMessage.PRICE_CODE,
                    responseCode = IAPErrorMessage.PRICE_CODE,
                    errorMessage = "Product SKU is not provided in the request."
                )
            }
        }
    }

    fun startPurchaseFlow() {
        upgradeNowClickedEvent()
        purchaseFlowData.flowStartTime = getCurrentTime()
        purchaseFlowData.takeIf { purchaseFlowData.courseName != null && it.productInfo != null }
            ?.apply {
                _uiState.value = IAPUIState.Loading(
                    courseName = purchaseFlowData.courseName!!,
                    loaderType = IAPLoaderType.PURCHASE_FLOW,
                )
                viewModelScope.launch {
                    runCatching {
                        iapInteractor.addToBasketAndCheckout(productInfo?.courseSku!!)
                    }.onSuccess { basketId ->
                        purchaseFlowData.basketId = basketId
                        _uiState.value = IAPUIState.PurchaseProduct
                    }.onFailure {
                        it.printStackTrace()
                    }
                }
            } ?: run {
            updateErrorState(
                requestType = IAPErrorMessage.NO_SKU_CODE,
                responseCode = IAPErrorMessage.NO_SKU_CODE,
                errorMessage = ""
            )
        }
    }

    fun purchaseItem(activity: FragmentActivity) {
        viewModelScope.launch {
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
        viewModelScope.launch {
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
                    updateErrorState(
                        requestType = IAPErrorMessage.EXECUTE_ORDER_CODE,
                        responseCode = it.httpErrorCode,
                        errorMessage = it.errorMessage,
                    )
                }
            }
        }
    }

    private fun consumeOrderForFurtherPurchases(purchaseFlowData: PurchaseFlowData) {
        viewModelScope.launch {
            purchaseFlowData.purchaseToken?.let {
                runCatching {
                    iapInteractor.consumePurchase(it)
                }.onSuccess {
                    updateCourseData()
                }.onFailure {
                    if (it is IAPException) {
                        updateErrorState(
                            requestType = IAPErrorMessage.CONSUME_CODE,
                            responseCode = it.httpErrorCode,
                            errorMessage = it.errorMessage,
                        )
                    }
                }
            }
        }
    }

    private fun updateCourseData() {
        viewModelScope.launch {
            purchaseFlowData.courseId?.let { courseId ->
                iapNotifier.send(UpdateCourseData(courseId))
            }
        }
    }

    fun showFeedbackScreen(context: Context, message: String) {
        EmailUtil.showFeedbackScreen(
            context = context,
            feedbackEmailAddress = config.getFeedbackEmailAddress(),
            feedback = message,
            appVersion = versionName
        )
        logIAPEvent(IAPAnalyticsEvent.IAP_ERROR_ALERT_ACTION, buildMap {
            put(IAPAnalyticsKeys.ERROR.key, message)
            put(IAPAnalyticsKeys.ERROR_ACTION.key, IAPAnalyticsKeys.GET_HELP.key)
        }.toMutableMap())
    }

    private fun upgradeNowClickedEvent() {
        logIAPEvent(IAPAnalyticsEvent.IAP_UPGRADE_NOW_CLICKED)
    }

    fun upgradeSuccessEvent() {
        val elapsedTime = getCurrentTime() - purchaseFlowData.flowStartTime
        logIAPEvent(IAPAnalyticsEvent.IAP_COURSE_UPGRADE_SUCCESS, buildMap {
            put(IAPAnalyticsKeys.ELAPSED_TIME.key, elapsedTime)
        }.toMutableMap())
    }

    fun purchaseErrorEvent(error: String) {
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

    private fun logIAPEvent(
        event: IAPAnalyticsEvent,
        params: MutableMap<String, Any?> = mutableMapOf()
    ) {
        analytics.logEvent(event.eventName, params.apply {
            put(IAPAnalyticsKeys.NAME.key, event.biValue)
            purchaseFlowData.let {
                put(IAPAnalyticsKeys.COURSE_ID.key, purchaseFlowData.courseId)
                put(
                    IAPAnalyticsKeys.PACING.key,
                    if (purchaseFlowData.isSelfPaced == true) IAPAnalyticsKeys.SELF.key else IAPAnalyticsKeys.INSTRUCTOR.key
                )
            }
            purchaseFlowData.formattedPrice?.let { formattedPrice ->
                put(IAPAnalyticsKeys.PRICE.key, formattedPrice)
            }
            purchaseFlowData.componentId?.let { componentId ->
                put(IAPAnalyticsKeys.COMPONENT_ID.key, componentId)
            }
            put(IAPAnalyticsKeys.SCREEN_NAME.key, purchaseFlowData.screenName)
            put(IAPAnalyticsKeys.CATEGORY.key, IAPAnalyticsKeys.IN_APP_PURCHASES.key)
        })
    }

    fun clearIAPFLow() {
        _uiState.value = IAPUIState.Clear
        purchaseFlowData = PurchaseFlowData()
    }

    fun isInProgress(): Boolean {
        return purchaseFlowData.courseId != null && purchaseFlowData.purchaseToken != null && purchaseFlowData.basketId != -1L
    }

    companion object {
        fun getCurrentTime() = Calendar.getInstance().timeInMillis
    }
}
