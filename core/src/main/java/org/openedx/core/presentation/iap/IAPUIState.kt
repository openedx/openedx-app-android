package org.openedx.core.presentation.iap

import org.openedx.core.exception.iap.IAPException

sealed class IAPUIState {
    data class ProductData(val formattedPrice: String) : IAPUIState()
    data object PurchaseProduct : IAPUIState()
    data object PurchasesFulfillmentCompleted : IAPUIState()
    data object FakePurchasesFulfillmentCompleted : IAPUIState()
    data object CourseDataUpdated : IAPUIState()
    data class Loading(val loaderType: IAPLoaderType) : IAPUIState()
    data class Error(val iapException: IAPException) : IAPUIState()
    data object Clear : IAPUIState()
}

enum class IAPLoaderType {
    PRICE, PURCHASE_FLOW, FULL_SCREEN, RESTORE_PURCHASES
}

enum class IAPFlow(val value: String) {
    RESTORE("restore"),
    SILENT("silent"),
    USER_INITIATED("user_initiated");

    fun value(): String {
        return this.name.lowercase()
    }
}

enum class IAPAction(val action: String) {
    ACTION_USER_INITIATED("user_initiated"),
    ACTION_GET_HELP("get_help"),
    ACTION_CLOSE("close"),
    ACTION_RELOAD_PRICE("reload_price"),
    ACTION_REFRESH("refresh"),
    ACTION_RETRY("retry"),
    ACTION_UNFULFILLED("unfulfilled"),
    ACTION_RESTORE("restore"),
    ACTION_ERROR_CLOSE("error_close"),
    ACTION_COMPLETION("completion"),
    ACTION_OK("ok"),
    ACTION_RESTORE_PURCHASE_CANCEL("restore_purchase_cancel")
}

enum class IAPRequestType(val request: String) {
    // Custom Codes for request types
    PRICE_CODE("price_fetch"),
    ADD_TO_BASKET_CODE("basket"),
    CHECKOUT_CODE("checkout"),
    PAYMENT_SDK_CODE("payment"),
    EXECUTE_ORDER_CODE("execute"),
    NO_SKU_CODE("sku"),
    CONSUME_CODE("consume"),
    UNFULFILLED_CODE("unfulfilled"),
    RESTORE_CODE("restore"),
    UNKNOWN("unknown");

    override fun toString(): String {
        return request
    }
}
