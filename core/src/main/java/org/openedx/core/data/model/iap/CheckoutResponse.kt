package org.openedx.core.data.model.iap

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.iap.CheckoutResponse as CheckoutResponseDomain


data class CheckoutResponse(
    @SerializedName("payment_form_data") val paymentFormData: MutableMap<Any, Any>,
    @SerializedName("payment_page_url") val paymentPageUrl: String,
    @SerializedName("payment_processor") val paymentProcessor: String
) {
    fun mapToDomain(): CheckoutResponseDomain {
        return CheckoutResponseDomain
    }
}
