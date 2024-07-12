package org.openedx.course.presentation.handouts

sealed class HandoutsUIState {
    data class HTMLContent(val htmlContent: String) : HandoutsUIState()
    data object Error : HandoutsUIState()
}
