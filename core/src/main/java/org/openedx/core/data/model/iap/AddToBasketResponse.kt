package org.openedx.core.data.model.iap

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.iap.AddToBasketResponse as AddToBasketResponseDomain

data class AddToBasketResponse(
    @SerializedName("success") val success: String,
    @SerializedName("basket_id") val basketId: Long
) {
    fun mapToDomain(): AddToBasketResponseDomain {
        return AddToBasketResponseDomain(basketId)
    }
}
