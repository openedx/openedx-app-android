package org.openedx.course.presentation.unit.html

import org.openedx.core.presentation.global.ErrorType

sealed class HtmlUnitUIState {
    data object Initialization : HtmlUnitUIState()
    data object Loading : HtmlUnitUIState()
    data class Loaded(val jsonProgress: String? = null) : HtmlUnitUIState()
    data class Error(val errorType: ErrorType) : HtmlUnitUIState()
}
