package org.openedx.discovery.presentation.program

import org.openedx.core.UIMessage

sealed class ProgramUIState {
    data object Loading : ProgramUIState()
    data object Loaded : ProgramUIState()

    class CourseEnrolled(val courseId: String, val isEnrolled: Boolean) : ProgramUIState()

    class UiMessage(val uiMessage: UIMessage) : ProgramUIState()
}
