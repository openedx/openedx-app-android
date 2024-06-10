package org.openedx.core.presentation.settings.calendarsync

import org.openedx.core.domain.model.CourseDateBlock
import java.util.concurrent.atomic.AtomicReference

data class CalendarSyncUIState(
    val isCalendarSyncEnabled: Boolean = false,
    val calendarTitle: String = "",
    val courseDates: List<CourseDateBlock> = listOf(),
    val dialogType: CalendarSyncDialogType = CalendarSyncDialogType.NONE,
    val isSynced: Boolean = false,
    val checkForOutOfSync: AtomicReference<Boolean> = AtomicReference(false),
    val uiMessage: AtomicReference<String> = AtomicReference(""),
) {
    val isDialogVisible: Boolean
        get() = dialogType != CalendarSyncDialogType.NONE
}
