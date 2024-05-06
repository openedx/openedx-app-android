package org.openedx.core.presentation.iap

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
import org.openedx.core.data.repository.iap.IAPRepository
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.iap.IAPPurchaseFlowData
import org.openedx.core.exception.iap.IAPErrorMessage
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.module.billing.BillingProcessor
import org.openedx.core.module.billing.getCourseSku
import org.openedx.core.module.billing.getPriceAmount

class IAPViewModel(
    private val versionName: String,
    private val preferencesManager: CorePreferences,
    private val billingProcessor: BillingProcessor,
    private val repository: IAPRepository
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

    var purchaseFlowData: IAPPurchaseFlowData = IAPPurchaseFlowData()

    private val purchaseListeners = object : BillingProcessor.PurchaseListeners {
        override fun onPurchaseComplete(purchase: Purchase) {
            if (purchase.getCourseSku() == purchaseFlowData.course?.productInfo?.courseSku) {
                _uiState.value =
                    IAPUIState.Loading(IAPLoaderType.FULL_SCREEN, purchaseFlowData.course!!)
                purchaseFlowData.purchaseToken = purchase.purchaseToken
                executeOrder(purchaseFlowData)
            }
        }

        override fun onPurchaseCancel(responseCode: Int, message: String) {
            _uiState.value = IAPUIState.Error(
                course = purchaseFlowData.course!!,
                requestType = IAPErrorMessage.PAYMENT_SDK_CODE,
                throwable = IAPException(responseCode, message)
            )
        }
    }

    init {
        billingProcessor.setPurchaseListener(purchaseListeners)
    }

    fun loadPrice(course: EnrolledCourse) {
        purchaseFlowData = IAPPurchaseFlowData(course)
        if (isIAPEnabledForUser(isOddUserId = preferencesManager.user?.isOddUserId == true).not()) {
            _uiState.value =
                IAPUIState.Error(
                    course = purchaseFlowData.course!!,
                    requestType = IAPErrorMessage.IAP_DISABLED,
                    throwable = Exception()
                )
        }
        viewModelScope.launch {
            purchaseFlowData.course?.takeIf { it.productInfo != null }?.apply {
                _uiState.value = IAPUIState.Loading(IAPLoaderType.PRICE, purchaseFlowData.course!!)
                val response =
                    billingProcessor.querySyncDetails(this.productInfo?.storeSku!!)
                val productDetail = response.productDetailsList?.firstOrNull()
                val billingResult = response.billingResult

                when {
                    billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetail == null -> {
                        _uiState.value = IAPUIState.Error(
                            course = purchaseFlowData.course!!,
                            requestType = IAPErrorMessage.NO_SKU_CODE,
                            throwable = IAPException(
                                httpErrorCode = billingResult.responseCode,
                                errorMessage = billingResult.debugMessage
                            )
                        )
                    }

                    productDetail?.productId == this.productInfo.storeSku && productDetail.oneTimePurchaseOfferDetails != null -> {
                        purchaseFlowData.formattedPrice =
                            productDetail.oneTimePurchaseOfferDetails?.formattedPrice!!
                        purchaseFlowData.price =
                            productDetail.oneTimePurchaseOfferDetails?.getPriceAmount()!!
                        purchaseFlowData.currencyCode =
                            productDetail.oneTimePurchaseOfferDetails?.priceCurrencyCode!!
                        _uiState.value = IAPUIState.ProductData(
                            course = purchaseFlowData.course!!,
                            formattedPrice = purchaseFlowData.formattedPrice!!
                        )
                    }

                    else -> {
                        _uiState.value = IAPUIState.Error(
                            requestType = IAPErrorMessage.PRICE_CODE,
                            course = purchaseFlowData.course!!,
                            throwable = IAPException(
                                httpErrorCode = response.billingResult.responseCode,
                                errorMessage = response.billingResult.debugMessage,
                            )
                        )
                    }
                }
            } ?: run {
                _uiState.value =
                    IAPUIState.Error(
                        course = purchaseFlowData.course!!,
                        requestType = IAPErrorMessage.PRICE_CODE,
                        throwable = Exception()
                    )
            }
        }
    }

    fun startPurchaseFlow() {
        purchaseFlowData.course?.takeIf { it.productInfo != null }?.apply {
            _uiState.value = IAPUIState.Loading(
                loaderType = IAPLoaderType.PURCHASE_FLOW,
                course = purchaseFlowData.course!!
            )
            viewModelScope.launch {
                val basketResponse = repository.addToBasket(productInfo?.courseSku!!)
                purchaseFlowData.basketId = basketResponse.basketId
                repository.proceedCheckout(basketResponse.basketId)
                _uiState.value = IAPUIState.PurchaseProduct(course = purchaseFlowData.course!!)
            }
        } ?: run {
            _uiState.value =
                IAPUIState.Error(
                    course = purchaseFlowData.course!!,
                    requestType = IAPErrorMessage.NO_SKU_CODE,
                    throwable = Exception()
                )
        }
    }

    fun purchaseItem(activity: FragmentActivity) {
        viewModelScope.launch {
            takeIf {
                preferencesManager.user?.id != null && purchaseFlowData.course?.productInfo != null
            }?.apply {
                billingProcessor.purchaseItem(
                    activity,
                    preferencesManager.user?.id!!,
                    purchaseFlowData.course?.productInfo!!
                )
            }
        }
    }

    private fun executeOrder(purchaseFlowData: IAPPurchaseFlowData) {
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

    private fun consumeOrderForFurtherPurchases(purchaseFlowData: IAPPurchaseFlowData) {
        viewModelScope.launch {
            purchaseFlowData.purchaseToken?.let {
                val result = billingProcessor.consumePurchase(it)
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _uiState.value = IAPUIState.PurchaseComplete
                } else {
                    _uiState.value = IAPUIState.Error(
                        course = purchaseFlowData.course!!,
                        requestType = IAPErrorMessage.CONSUME_CODE,
                        throwable = IAPException(
                            httpErrorCode = result.responseCode,
                            errorMessage = result.debugMessage,
                        )
                    )
                }
            }
        }
    }

    fun clearIAPFLow() {
        _uiState.value = IAPUIState.Clear
        purchaseFlowData = IAPPurchaseFlowData()
    }

    override fun onCleared() {
        super.onCleared()
        billingProcessor.release()
    }
}
