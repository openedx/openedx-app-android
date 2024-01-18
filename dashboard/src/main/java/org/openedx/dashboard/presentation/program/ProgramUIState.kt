package org.openedx.dashboard.presentation.program

import org.openedx.core.UIMessage

sealed class ProgramUIState {
    object Loading : ProgramUIState()

    class UiMessage(val uiMessage: UIMessage) : ProgramUIState()
}
