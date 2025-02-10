package org.openedx.discovery.presentation.program

import org.openedx.core.presentation.global.ErrorType
import org.openedx.foundation.presentation.UIMessage

sealed class ProgramUIState {
    data object Loading : ProgramUIState()
    data object Loaded : ProgramUIState()
    data class Error(val errorType: ErrorType) : ProgramUIState()

    class CourseEnrolled(val courseId: String, val isEnrolled: Boolean) : ProgramUIState()

    class UiMessage(val uiMessage: UIMessage) : ProgramUIState()
}
