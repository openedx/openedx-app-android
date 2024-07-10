package org.openedx.core.presentation.iap

import org.openedx.core.R

/**
 * 400 - BAD_REQUEST
 * 403 - FORBIDDEN
 * 406 - NOT_ACCEPTABLE
 * 409 - CONFLICT
 * General - GENERAL
 * */
enum class IAPErrorDialogType(
    val messageResId: Int = 0,
    val positiveButtonResId: Int = 0,
    val negativeButtonResId: Int = 0,
    val neutralButtonResId: Int = 0,
) {
    PRICE_ERROR_DIALOG(
        R.string.iap_error_price_not_fetched,
        R.string.core_error_try_again,
        R.string.core_cancel
    ),
    NO_SKU_ERROR_DIALOG,
    ADD_TO_BASKET_BAD_REQUEST_ERROR_DIALOG(
        R.string.iap_course_not_available_message,
        R.string.core_cancel,
        R.string.iap_get_help
    ),
    ADD_TO_BASKET_FORBIDDEN_ERROR_DIALOG(
        R.string.iap_unauthenticated_account_message,
        R.string.core_cancel,
        R.string.iap_get_help
    ),
    ADD_TO_BASKET_NOT_ACCEPTABLE_ERROR_DIALOG(
        R.string.iap_course_already_paid_for_message,
        R.string.iap_label_refresh_now,
        R.string.core_cancel
    ),
    ADD_TO_BASKET_GENERAL_ERROR_DIALOG(
        R.string.iap_general_upgrade_error_message,
        R.string.core_cancel,
        R.string.iap_get_help
    ),
    CHECKOUT_BAD_REQUEST_ERROR_DIALOG(
        R.string.iap_payment_could_not_be_processed,
        R.string.core_cancel,
        R.string.iap_get_help
    ),
    CHECKOUT_FORBIDDEN_ERROR_DIALOG(
        R.string.iap_unauthenticated_account_message,
        R.string.core_cancel,
        R.string.iap_get_help
    ),
    CHECKOUT_NOT_ACCEPTABLE_ERROR_DIALOG(
        R.string.iap_course_not_available_message,
        R.string.iap_label_refresh_now,
        R.string.core_cancel
    ),
    CHECKOUT_GENERAL_ERROR_DIALOG(
        R.string.iap_general_upgrade_error_message,
        R.string.core_cancel,
        R.string.iap_get_help
    ),
    EXECUTE_BAD_REQUEST_ERROR_DIALOG(
        R.string.iap_course_not_fullfilled,
        R.string.iap_refresh_to_retry,
        R.string.iap_get_help,
        R.string.core_cancel
    ),
    EXECUTE_FORBIDDEN_ERROR_DIALOG(
        R.string.iap_course_not_fullfilled,
        R.string.iap_refresh_to_retry,
        R.string.iap_get_help,
        R.string.core_cancel
    ),
    EXECUTE_NOT_ACCEPTABLE_ERROR_DIALOG(
        R.string.iap_course_already_paid_for_message,
        R.string.iap_label_refresh_now,
        R.string.iap_get_help,
        R.string.core_cancel
    ),
    EXECUTE_CONFLICT_ERROR_DIALOG(
        R.string.iap_course_already_paid_for_message,
        R.string.core_cancel,
        R.string.iap_get_help
    ),
    EXECUTE_GENERAL_ERROR_DIALOG(
        R.string.iap_general_upgrade_error_message,
        R.string.iap_refresh_to_retry,
        R.string.iap_get_help,
        R.string.core_cancel
    ),
    CONSUME_ERROR_DIALOG(
        R.string.iap_course_not_fullfilled,
        R.string.iap_refresh_to_retry,
        R.string.iap_get_help,
        R.string.core_cancel
    ),
    PAYMENT_SDK_ERROR_DIALOG(
        R.string.iap_payment_could_not_be_processed,
        R.string.core_cancel,
        R.string.iap_get_help
    ),
    GENERAL_DIALOG_ERROR(
        R.string.iap_general_upgrade_error_message,
        R.string.core_cancel,
        R.string.iap_get_help
    ),
    NONE;
}
