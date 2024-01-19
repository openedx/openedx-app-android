package org.openedx.dashboard.presentation.program

import org.openedx.core.UIMessage

sealed class ProgramUIState {
    object Loading : ProgramUIState()
    object Loaded : ProgramUIState()

    class CourseEnrolled(val courseId: String, val isEnrolled: Boolean) : ProgramUIState()

    class UiMessage(val uiMessage: UIMessage) : ProgramUIState()
}
