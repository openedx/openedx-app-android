package org.openedx.core.data.repository.iap

import org.openedx.core.ApiConstants
import org.openedx.core.data.api.iap.InAppPurchasesApi
import org.openedx.core.domain.model.iap.AddToBasketResponse
import org.openedx.core.domain.model.iap.CheckoutResponse
import org.openedx.core.domain.model.iap.ExecuteOrderResponse
import org.openedx.core.exception.iap.IAPException
import org.openedx.core.exception.iap.getMessage
import org.openedx.core.presentation.iap.IAPRequestType

class IAPRepository(private val api: InAppPurchasesApi) {

    suspend fun addToBasket(courseSku: String): AddToBasketResponse {
        val response = api.addToBasket(courseSku)
        if (response.isSuccessful) {
            response.body()?.run {
                return mapToDomain()
            }
        }
        throw IAPException(
            requestType = IAPRequestType.ADD_TO_BASKET_CODE,
            httpErrorCode = response.code(),
            errorMessage = response.getMessage()
        )
    }

    suspend fun proceedCheckout(basketId: Long): CheckoutResponse {
        val response = api.proceedCheckout(
            basketId = basketId,
            paymentProcessor = ApiConstants.IAPFields.PAYMENT_PROCESSOR
        )
        if (response.isSuccessful) {
            response.body()?.run {
                return mapToDomain()
            }
        }
        throw IAPException(
            requestType = IAPRequestType.CHECKOUT_CODE,
            httpErrorCode = response.code(),
            errorMessage = response.getMessage()
        )
    }

    suspend fun executeOrder(
        basketId: Long,
        paymentProcessor: String,
        purchaseToken: String,
        price: Double,
        currencyCode: String,
    ): ExecuteOrderResponse {
        val response = api.executeOrder(
            basketId = basketId,
            paymentProcessor = paymentProcessor,
            purchaseToken = purchaseToken,
            price = price,
            currencyCode = currencyCode
        )
        if (response.isSuccessful) {
            response.body()?.run {
                return mapToDomain()
            }
        }
        throw IAPException(
            requestType = IAPRequestType.EXECUTE_ORDER_CODE,
            httpErrorCode = response.code(),
            errorMessage = response.getMessage()
        )
    }
}
