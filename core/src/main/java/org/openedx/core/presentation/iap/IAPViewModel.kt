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
import org.openedx.core.ApiConstants
import org.openedx.core.BaseViewModel
import org.openedx.core.config.Config
import org.openedx.core.data.repository.iap.IAPRepository
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.ProductInfo
import org.openedx.core.domain.model.iap.PurchaseFlowData
import org.openedx.core.exception.iap.IAPErrorMessage
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.module.billing.getCourseSku
import org.openedx.core.module.billing.getPriceAmount
import org.openedx.core.presentation.IAPAnalytics
import org.openedx.core.presentation.IAPAnalyticsEvent
import org.openedx.core.presentation.IAPAnalyticsKeys
import org.openedx.core.utils.EmailUtil
import java.util.Calendar

class IAPViewModel(
    private val screenName: String,
    private val versionName: String,
    private val preferencesManager: CorePreferences,
    private val billingProcessor: BillingProcessor,
    private val repository: IAPRepository,
    private val analytics: IAPAnalytics,
    private val config: Config
) : BaseViewModel() {

    private val iapConfig
        get() = preferencesManager.appConfig.iapConfig

    private val isIAPEnabled
        get() = iapConfig.isEnabled &&
                iapConfig.disableVersions.contains(versionName).not()

    private fun isIAPEnabledForUser(isOddUserId: Boolean): Boolean {
        if (isIAPEnabled) {
            if (iapConfig.isExperimentEnabled) {
                return isOddUserId
            }
            return true
        }
        return false
    }

    private val _uiState = MutableStateFlow<IAPUIState>(IAPUIState.Clear)
    val uiState: StateFlow<IAPUIState>
        get() = _uiState.asStateFlow()

    var purchaseFlowData: PurchaseFlowData = PurchaseFlowData()

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

    init {
        billingProcessor.setPurchaseListener(purchaseListeners)
    }

    fun loadPrice(
        courseId: String,
        courseName: String = "",
        isSelfPaced: Boolean = false,
        productInfo: ProductInfo
    ) {
        purchaseFlowData = PurchaseFlowData(
            courseId = courseId,
            courseName = courseName,
            isSelfPaced = isSelfPaced,
            productInfo = productInfo
        )
        if (isIAPEnabledForUser(isOddUserId = preferencesManager.user?.isOddUserId == true).not()) {
            updateErrorState(
                requestType = IAPErrorMessage.IAP_DISABLED,
                responseCode = IAPErrorMessage.IAP_DISABLED,
                errorMessage = ""
            )
        }
        viewModelScope.launch {
            purchaseFlowData.takeIf { purchaseFlowData.courseName != null && it.productInfo != null }
                ?.apply {
                    _uiState.value = IAPUIState.Loading(
                        courseName = purchaseFlowData.courseName!!,
                        loaderType = IAPLoaderType.PRICE
                    )
                    val response =
                        billingProcessor.querySyncDetails(this.productInfo?.storeSku!!)
                    val productDetail = response.productDetailsList?.firstOrNull()
                    val billingResult = response.billingResult

                    when {
                        billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetail == null -> {
                            updateErrorState(
                                requestType = IAPErrorMessage.NO_SKU_CODE,
                                responseCode = billingResult.responseCode,
                                errorMessage = billingResult.debugMessage
                            )
                        }

                        productDetail?.productId == this.productInfo.storeSku && productDetail.oneTimePurchaseOfferDetails != null -> {
                            purchaseFlowData.formattedPrice =
                                productDetail.oneTimePurchaseOfferDetails?.formattedPrice!!
                            purchaseFlowData.price =
                                productDetail.oneTimePurchaseOfferDetails?.getPriceAmount()!!
                            purchaseFlowData.currencyCode =
                                productDetail.oneTimePurchaseOfferDetails?.priceCurrencyCode!!
                            purchaseFlowData.courseName?.let {
                                _uiState.value = IAPUIState.ProductData(
                                    courseName = it,
                                    formattedPrice = purchaseFlowData.formattedPrice!!
                                )
                            }
                        }

                        else -> {
                            updateErrorState(
                                requestType = IAPErrorMessage.PRICE_CODE,
                                responseCode = response.billingResult.responseCode,
                                errorMessage = response.billingResult.debugMessage,
                            )
                        }
                    }
                } ?: run {
                updateErrorState(
                    requestType = IAPErrorMessage.PRICE_CODE,
                    responseCode = IAPErrorMessage.PRICE_CODE,
                    errorMessage = ""
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
                    val basketResponse = repository.addToBasket(productInfo?.courseSku!!)
                    purchaseFlowData.basketId = basketResponse.basketId
                    repository.proceedCheckout(basketResponse.basketId)
                    _uiState.value = IAPUIState.PurchaseProduct
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
        _uiState.value = IAPUIState.Loading(
            courseName = purchaseFlowData.courseName!!,
            loaderType = IAPLoaderType.PURCHASE_FLOW,
        )
        viewModelScope.launch {
            takeIf {
                preferencesManager.user?.id != null && purchaseFlowData.productInfo != null
            }?.apply {
                billingProcessor.purchaseItem(
                    activity,
                    preferencesManager.user?.id!!,
                    purchaseFlowData.productInfo!!
                )
            }
        }
    }

    private fun executeOrder(purchaseFlowData: PurchaseFlowData) {
        viewModelScope.launch {
            repository.executeOrder(
                basketId = purchaseFlowData.basketId,
                paymentProcessor = ApiConstants.IAPFields.PAYMENT_PROCESSOR,
                purchaseToken = purchaseFlowData.purchaseToken!!,
                price = purchaseFlowData.price,
                currencyCode = purchaseFlowData.currencyCode,
            )
            consumeOrderForFurtherPurchases(purchaseFlowData)
        }
    }

    private fun consumeOrderForFurtherPurchases(purchaseFlowData: PurchaseFlowData) {
        viewModelScope.launch {
            purchaseFlowData.purchaseToken?.let {
                val result = billingProcessor.consumePurchase(it)
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _uiState.value = IAPUIState.FlowComplete
                } else {
                    updateErrorState(
                        requestType = IAPErrorMessage.CONSUME_CODE,
                        responseCode = result.responseCode,
                        errorMessage = result.debugMessage,
                    )
                }
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
            put(IAPAnalyticsKeys.SCREEN_NAME.key, screenName)
            put(IAPAnalyticsKeys.CATEGORY.key, IAPAnalyticsKeys.IN_APP_PURCHASES.key)
        })
    }

    fun clearIAPFLow() {
        _uiState.value = IAPUIState.Clear
        purchaseFlowData = PurchaseFlowData()
    }

    override fun onCleared() {
        super.onCleared()
        billingProcessor.release()
    }

    fun isInProgress(): Boolean {
        return purchaseFlowData.courseId != null && purchaseFlowData.purchaseToken != null && purchaseFlowData.basketId != -1L
    }

    companion object {
        fun getCurrentTime() = Calendar.getInstance().timeInMillis
    }
}
