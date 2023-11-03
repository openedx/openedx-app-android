package org.openedx.core.presentation.dialog.selectorbottomsheet

import androidx.lifecycle.viewModelScope
import org.openedx.core.BaseViewModel
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseSubtitleLanguageChanged
import kotlinx.coroutines.launch

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
