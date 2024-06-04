package org.openedx.core.presentation.iap

sealed class IAPUIState {
    data class ProductData(val courseName: String, val formattedPrice: String) : IAPUIState()
    data object PurchaseProduct : IAPUIState()
    data object FlowComplete : IAPUIState()
    data object CourseDataUpdated : IAPUIState()
    data class Loading(val courseName: String, val loaderType: IAPLoaderType) : IAPUIState()
    data class Error(
        val courseName: String,
        val requestType: Int = -1,
        val feedbackErrorMessage: String = ""
    ) : IAPUIState()

    data object Clear : IAPUIState()
}

enum class IAPAction {
    LOAD_PRICE, START_PURCHASE_FLOW, PURCHASE_PRODUCT, FLOW_COMPLETE, CLEAR
}

enum class IAPLoaderType {
    PRICE, PURCHASE_FLOW, FULL_SCREEN
}

enum class IAPFlow {
    RESTORE,
    SILENT,
    USER_INITIATED;

    fun value(): String {
        return this.name.lowercase()
    }

    fun isSilentMode() = (this == RESTORE || this == SILENT)
}
