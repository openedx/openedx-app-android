package org.openedx.core.presentation.dialog.selectorbottomsheet

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseSubtitleLanguageChanged
import org.openedx.foundation.presentation.BaseViewModel

class SelectDialogViewModel(
    private val notifier: CourseNotifier
) : BaseViewModel() {

    var values = mutableListOf<RegistrationField.Option>()

    fun sendCourseEventChanged(value: String) {
        viewModelScope.launch {
            notifier.send(CourseSubtitleLanguageChanged(value))
        }
    }
}
