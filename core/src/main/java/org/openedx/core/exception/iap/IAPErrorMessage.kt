package org.openedx.core.exception.iap

/**
 * Error handling model class for ViewModel
 * handle exceptions/error based on requestType
 */
data class IAPErrorMessage(
    val requestType: Int = 0,
    val throwable: Throwable
) {
    companion object {
        // Custom Codes for request types
        const val ADD_TO_BASKET_CODE = 0x201
        const val CHECKOUT_CODE = 0x202
        const val EXECUTE_ORDER_CODE = 0x203
        const val PAYMENT_SDK_CODE = 0x204
        const val COURSE_REFRESH_CODE = 0x205
        const val PRICE_CODE = 0x206
        const val NO_SKU_CODE = 0x207
        const val CONSUME_CODE = 0x208
        const val IAP_DISABLED = 0x209
    }
}
