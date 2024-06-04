package org.openedx.core.domain.interactor

import androidx.fragment.app.FragmentActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import org.openedx.core.ApiConstants
import org.openedx.core.data.repository.iap.IAPRepository
import org.openedx.core.domain.ProductInfo
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.module.billing.BillingProcessor

class IAPInteractor(
    private val billingProcessor: BillingProcessor,
    private val repository: IAPRepository,
) {

    suspend fun loadPrice(productId: String): ProductDetails.OneTimePurchaseOfferDetails {
        val response =
            billingProcessor.querySyncDetails(productId)
        val productDetail = response.productDetailsList?.firstOrNull()
        val billingResult = response.billingResult
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetail?.oneTimePurchaseOfferDetails != null) {
            return productDetail.oneTimePurchaseOfferDetails!!
        } else {
            throw IAPException(
                httpErrorCode = billingResult.responseCode,
                errorMessage = billingResult.debugMessage
            )
        }
    }

    suspend fun addToBasketAndCheckout(courseSku: String): Long {
        val basketResponse = repository.addToBasket(courseSku)
        repository.proceedCheckout(basketResponse.basketId)
        return basketResponse.basketId
    }

    suspend fun purchaseItem(
        activity: FragmentActivity,
        id: Long,
        productInfo: ProductInfo,
        purchaseListeners: BillingProcessor.PurchaseListeners,
    ) {
        billingProcessor.setPurchaseListener(purchaseListeners)
        billingProcessor.purchaseItem(activity, id, productInfo)
    }

    suspend fun consumePurchase(it: String) {
        val result = billingProcessor.consumePurchase(it)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            throw IAPException(result.responseCode, result.debugMessage)
        }
    }

    suspend fun executeOrder(
        basketId: Long,
        purchaseToken: String,
        price: Double,
        currencyCode: String
    ) {
        repository.executeOrder(
            basketId = basketId,
            paymentProcessor = ApiConstants.IAPFields.PAYMENT_PROCESSOR,
            purchaseToken = purchaseToken,
            price = price,
            currencyCode = currencyCode,
        )
    }
}
