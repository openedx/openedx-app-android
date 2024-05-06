package org.openedx.core.data.api.iap

import org.openedx.core.data.model.iap.CheckoutResponse
import org.edx.mobile.model.iap.ExecuteOrderResponse
import org.openedx.core.data.model.iap.AddToBasketResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface InAppPurchasesApi {
    @GET("/api/iap/v1/basket/add/")
    suspend fun addToBasket(@Query("sku") productId: String): AddToBasketResponse

    @FormUrlEncoded
    @POST("/api/iap/v1/checkout/")
    suspend fun proceedCheckout(
        @Field("basket_id") basketId: Long,
        @Field("payment_processor") paymentProcessor: String
    ): CheckoutResponse

    @FormUrlEncoded
    @POST("/api/iap/v1/execute/")
    suspend fun executeOrder(
        @Field("basket_id") basketId: Long,
        @Field("payment_processor") paymentProcessor: String,
        @Field("purchase_token") purchaseToken: String,
        @Field("price") price: Double,
        @Field("currency_code") currencyCode: String,
    ): ExecuteOrderResponse
}
