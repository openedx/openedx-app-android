package org.openedx.profile.presentation.calendar

import androidx.activity.result.ActivityResultLauncher
import org.openedx.core.BaseViewModel
import org.openedx.core.system.CalendarManager

class CalendarViewModel(
    private val calendarManager: CalendarManager
) : BaseViewModel() {

    fun setUpCalendarSync(permissionLauncher: ActivityResultLauncher<Array<String>>) {
        if (!calendarManager.hasPermissions()) {
            permissionLauncher.launch(calendarManager.permissions)
        } else {

        }
    }
}
