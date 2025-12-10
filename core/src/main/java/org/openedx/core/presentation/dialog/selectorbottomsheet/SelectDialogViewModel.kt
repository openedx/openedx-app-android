package org.openedx.core.presentation.dialog.selectorbottomsheet

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseSubtitleLanguageChanged
import org.openedx.foundation.presentation.BaseViewModel
import org.openedx.foundation.system.ResourceManager

class SelectDialogViewModel(
    private val notifier: CourseNotifier,
    private val resourceManager: ResourceManager,
) : BaseViewModel(resourceManager) {

    var values = mutableListOf<RegistrationField.Option>()

    fun sendCourseEventChanged(value: String) {
        viewModelScope.launch {
            notifier.send(CourseSubtitleLanguageChanged(value))
        }
    }
}
