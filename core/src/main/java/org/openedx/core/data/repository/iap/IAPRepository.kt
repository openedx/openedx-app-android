package org.openedx.core.data.repository.iap

import org.openedx.core.ApiConstants
import org.openedx.core.data.api.iap.InAppPurchasesApi
import org.openedx.core.domain.model.iap.AddToBasketResponse
import org.openedx.core.domain.model.iap.CheckoutResponse
import org.openedx.core.domain.model.iap.ExecuteOrderResponse

class IAPRepository(private val api: InAppPurchasesApi) {
    suspend fun addToBasket(courseSku: String): AddToBasketResponse {
        return api.addToBasket(courseSku).mapToDomain()
    }

    suspend fun proceedCheckout(basketId: Long): CheckoutResponse {
        return api.proceedCheckout(
            basketId = basketId,
            paymentProcessor = ApiConstants.IAPFields.PAYMENT_PROCESSOR
        ).mapToDomain()
    }

    suspend fun executeOrder(
        basketId: Long,
        paymentProcessor: String,
        purchaseToken: String,
        price: Double,
        currencyCode: String,
    ): ExecuteOrderResponse {
        return api.executeOrder(
            basketId = basketId,
            paymentProcessor = paymentProcessor,
            purchaseToken = purchaseToken,
            price = price,
            currencyCode = currencyCode
        ).mapToDomain()
    }
}
