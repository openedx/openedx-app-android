package org.openedx.discovery.presentation.program

import org.openedx.core.UIMessage

sealed class ProgramUIState {
    data object Loading : ProgramUIState()
    data object Loaded : ProgramUIState()
    data object Error : ProgramUIState()

    class CourseEnrolled(val courseId: String, val isEnrolled: Boolean) : ProgramUIState()

    class UiMessage(val uiMessage: UIMessage) : ProgramUIState()
}

enum class ProgramUIAction {
    CHECK_INTERNET_CONNECTION,
    WEB_PAGE_LOADED,
    WEB_PAGE_ERROR,
    ON_RELOAD
}
