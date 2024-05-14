package org.openedx.core.exception.iap

import android.text.TextUtils
import java.util.Locale

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

        /**
         * Returns a StringBuilder containing the formatted error message.
         * i.e Error: error_endpoint-error_code-error_message
         *
         * @param requestType  Call endpoint
         * @param errorCode    HTTP or SDK response code
         * @param errorMessage Error message from HTTP call or SDK
         * @return Formatted error message.
         */
        fun getFormattedErrorMessage(
            requestType: Int,
            errorCode: Int,
            errorMessage: String?
        ): StringBuilder {
            val body = StringBuilder()
            if (requestType == 0) {
                return body
            }
            val endpoint = when (requestType) {
                ADD_TO_BASKET_CODE -> "basket"
                CHECKOUT_CODE -> "checkout"
                EXECUTE_ORDER_CODE -> "execute"
                PAYMENT_SDK_CODE -> "payment"
                PRICE_CODE -> "price"
                NO_SKU_CODE -> "sku"
                CONSUME_CODE -> "consume"
                else -> "unhandledError"
            }
            body.append(String.format("%s", endpoint))
            // change the default value to -1 cuz in case of BillingClient return errorCode 0 for price load.
            if (errorCode == -1) {
                return body
            }
            body.append(String.format(Locale.ENGLISH, "-%d", errorCode))
            if (!TextUtils.isEmpty(errorMessage)) body.append(String.format("-%s", errorMessage))
            return body
        }
    }
}
