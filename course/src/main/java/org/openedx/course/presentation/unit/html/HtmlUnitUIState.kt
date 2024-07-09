package org.openedx.course.presentation.unit.html

import org.openedx.core.presentation.global.ErrorType

sealed class HtmlUnitUIState {
    data object Loading : HtmlUnitUIState()
    data object Loaded : HtmlUnitUIState()
    data class Error(val errorType: ErrorType) : HtmlUnitUIState()
}
