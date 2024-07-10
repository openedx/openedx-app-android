package org.openedx.core.exception.iap

import android.text.TextUtils
import com.android.billingclient.api.BillingClient
import org.json.JSONObject
import org.openedx.core.presentation.iap.IAPErrorDialogType
import org.openedx.core.presentation.iap.IAPRequestType
import retrofit2.Response
import java.util.Locale

/**
 *
 * Signals that the user unable to complete the in-app purchases follow it being not parsable or
 * incomplete according to what we expect.
 *
 * @param requestType stores the request type for exception occurs.
 * @param httpErrorCode stores the error codes can be either [BillingClient][com.android.billingclient.api.BillingClient]
 *                      OR http error codes for ecommerce end-points, and setting it up to `-1`
 *                      cause some at some service return error code `0`.
 * @param errorMessage stores the error messages received from BillingClient & ecommerce end-points.
 * */
class IAPException(
    val requestType: IAPRequestType = IAPRequestType.UNKNOWN,
    val httpErrorCode: Int = DEFAULT_HTTP_ERROR_CODE,
    val errorMessage: String
) : Exception(errorMessage) {

    /**
     * Returns a StringBuilder containing the formatted error message.
     * i.e Error: error_endpoint-error_code-error_message
     *
     * @return Formatted error message.
     */
    fun getFormattedErrorMessage(): String {
        val body = StringBuilder()
        if (requestType == IAPRequestType.UNKNOWN) {
            return body.toString()
        }
        body.append(String.format("%s", requestType.request))
        // change the default value to -1 cuz in case of BillingClient return errorCode 0 for price load.
        if (httpErrorCode == DEFAULT_HTTP_ERROR_CODE) {
            return body.toString()
        }
        body.append(String.format(Locale.ENGLISH, "-%d", httpErrorCode))
        if (!TextUtils.isEmpty(errorMessage)) body.append(String.format("-%s", errorMessage))
        return body.toString()
    }

    fun getIAPErrorDialogType(): IAPErrorDialogType {
        return when (requestType) {
            IAPRequestType.PRICE_CODE -> {
                IAPErrorDialogType.PRICE_ERROR_DIALOG
            }

            IAPRequestType.NO_SKU_CODE -> {
                IAPErrorDialogType.NO_SKU_ERROR_DIALOG
            }

            IAPRequestType.ADD_TO_BASKET_CODE -> {
                when (httpErrorCode) {
                    400 -> {
                        IAPErrorDialogType.ADD_TO_BASKET_BAD_REQUEST_ERROR_DIALOG
                    }

                    403 -> {
                        IAPErrorDialogType.ADD_TO_BASKET_FORBIDDEN_ERROR_DIALOG
                    }

                    406 -> {
                        IAPErrorDialogType.ADD_TO_BASKET_NOT_ACCEPTABLE_ERROR_DIALOG
                    }

                    else -> {
                        IAPErrorDialogType.ADD_TO_BASKET_GENERAL_ERROR_DIALOG
                    }
                }
            }

            IAPRequestType.CHECKOUT_CODE -> {
                when (httpErrorCode) {
                    400 -> {
                        IAPErrorDialogType.CHECKOUT_BAD_REQUEST_ERROR_DIALOG
                    }

                    403 -> {
                        IAPErrorDialogType.CHECKOUT_FORBIDDEN_ERROR_DIALOG
                    }

                    406 -> {
                        IAPErrorDialogType.CHECKOUT_NOT_ACCEPTABLE_ERROR_DIALOG
                    }

                    else -> {
                        IAPErrorDialogType.CHECKOUT_GENERAL_ERROR_DIALOG
                    }

                }
            }

            IAPRequestType.EXECUTE_ORDER_CODE -> {
                when (httpErrorCode) {
                    400 -> {
                        IAPErrorDialogType.EXECUTE_BAD_REQUEST_ERROR_DIALOG
                    }

                    403 -> {
                        IAPErrorDialogType.EXECUTE_FORBIDDEN_ERROR_DIALOG
                    }

                    406 -> {
                        IAPErrorDialogType.EXECUTE_NOT_ACCEPTABLE_ERROR_DIALOG
                    }

                    409 -> {
                        IAPErrorDialogType.EXECUTE_CONFLICT_ERROR_DIALOG
                    }

                    else -> {
                        IAPErrorDialogType.EXECUTE_GENERAL_ERROR_DIALOG
                    }
                }
            }

            IAPRequestType.CONSUME_CODE -> {
                IAPErrorDialogType.CONSUME_ERROR_DIALOG
            }

            IAPRequestType.PAYMENT_SDK_CODE -> {
                if (httpErrorCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
                    IAPErrorDialogType.PAYMENT_SDK_ERROR_DIALOG
                } else {
                    IAPErrorDialogType.GENERAL_DIALOG_ERROR
                }
            }

            else -> {
                IAPErrorDialogType.GENERAL_DIALOG_ERROR
            }
        }
    }

    companion object {
        private const val DEFAULT_HTTP_ERROR_CODE = -1
    }
}

/**
 * Attempts to extract error message from api responses and fails gracefully if unable to do so.
 *
 * @return extracted text message; null if no message was received or was unable to parse it.
 */
fun <T> Response<T>.getMessage(): String {
    if (isSuccessful) return message()
    return try {
        val errors = JSONObject(errorBody()?.string() ?: "{}")
        errors.optString("error")
    } catch (ex: Exception) {
        ""
    }
}
