package com.raccoongang.core.presentation.dialog

import androidx.lifecycle.viewModelScope
import com.raccoongang.core.BaseViewModel
import com.raccoongang.core.domain.model.RegistrationField
import com.raccoongang.core.system.notifier.CourseNotifier
import com.raccoongang.core.system.notifier.CourseSubtitleLanguageChanged
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
