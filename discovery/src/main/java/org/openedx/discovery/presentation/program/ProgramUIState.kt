package org.openedx.discovery.presentation.program

import org.openedx.core.UIMessage
import org.openedx.core.presentation.global.ErrorType

sealed class ProgramUIState {
    data object Loading : ProgramUIState()
    data object Loaded : ProgramUIState()
    data class Error(val errorType: ErrorType) : ProgramUIState()

    class CourseEnrolled(val courseId: String, val isEnrolled: Boolean) : ProgramUIState()

    class UiMessage(val uiMessage: UIMessage) : ProgramUIState()
}

enum class ProgramUIAction {
    WEB_PAGE_LOADED,
    WEB_PAGE_ERROR,
    RELOAD_WEB_PAGE
}
