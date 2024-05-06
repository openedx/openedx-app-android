package org.openedx.core.domain.model.iap

import org.openedx.core.domain.model.EnrolledCourse

data class IAPPurchaseFlowData(var course: EnrolledCourse? = null) {
    var currencyCode: String = ""
    var price: Double = 0.0
    var formattedPrice: String? = null
    var purchaseToken: String? = null
    var basketId: Long = -1
    fun clear() {
        course = null
        price = 0.0
        formattedPrice = null
        purchaseToken = null
        basketId = -1
    }
}
