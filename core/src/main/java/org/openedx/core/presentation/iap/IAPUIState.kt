package org.openedx.core.presentation.iap

import org.openedx.core.domain.model.EnrolledCourse

sealed class IAPUIState {
    data class ProductData(val course: EnrolledCourse, val formattedPrice: String) : IAPUIState()

    data class PurchaseProduct(val course: EnrolledCourse) : IAPUIState()

    data object PurchaseComplete : IAPUIState()
    data class Loading(val loaderType: IAPLoaderType, val course: EnrolledCourse) : IAPUIState()

    data class Error(
        val course: EnrolledCourse,
        val requestType: Int = -1,
        val throwable: Throwable
    ) : IAPUIState()

    data object Clear : IAPUIState()
}

enum class IAPAction {
    LOAD_PRICE, START_PURCHASE_FLOW, PURCHASE_PRODUCT, CLEAR
}

enum class IAPLoaderType {
    PRICE, PURCHASE_FLOW, FULL_SCREEN
}
