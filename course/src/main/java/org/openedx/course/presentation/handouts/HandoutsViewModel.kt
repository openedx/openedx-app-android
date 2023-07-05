package org.openedx.course.presentation.handouts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import org.openedx.core.BaseViewModel
import org.openedx.core.domain.model.AnnouncementModel
import org.openedx.core.domain.model.HandoutsModel
import org.openedx.course.domain.interactor.CourseInteractor
import kotlinx.coroutines.launch

class HandoutsViewModel(
    private val courseId: String,
    private val handoutsType: String,
    private val interactor: CourseInteractor
) : BaseViewModel() {

    private val _htmlContent = MutableLiveData<String>()
    val htmlContent: LiveData<String>
        get() = _htmlContent

    init {
        getEnrolledCourse()
    }

    private fun getEnrolledCourse() {
        viewModelScope.launch {
            try {
                if (HandoutsType.valueOf(handoutsType) == HandoutsType.Handouts) {
                    val handouts = interactor.getHandouts(courseId)
                    _htmlContent.value = handoutsToHtml(handouts)
                } else {
                    val announcements = interactor.getAnnouncements(courseId)
                    _htmlContent.value = announcementsToHtml(announcements)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handoutsToHtml(handoutsModel: HandoutsModel): String {
        val buff = StringBuilder()
        buff.apply {
            append("<body>")
            append("<div class=\"header\">")
            append(handoutsModel.handoutsHtml)
            append("</div>")
            append("</body>")
        }
        return buff.toString()
    }

    private fun announcementsToHtml(announcements: List<AnnouncementModel>): String {
        val buff = StringBuilder()
        buff.apply {
            append("<body>")
            for (model in announcements) {
                append("<div class=\"header\">")
                append("<br>")
                append(model.date)
                append("</div>")
                append("<div class=\"separator\"></div>")
                append("<div>")
                append(model.content)
                append("</div>")
            }
            append("</body>")
        }
        return buff.toString()
    }

    fun injectDarkMode(content: String, bgColor: ULong, textColor: ULong): String {
        val darkThemeStyle = "<style>\n" +
                "      body {\n" +
                "        background-color: #${getColorFromULong(bgColor)};\n" +
                "        color: #${getColorFromULong(textColor)};\n" +
                "      }\n" +
                "    </style>"
        val buff = StringBuffer().apply {
            if (bgColor != ULong.MIN_VALUE) append(darkThemeStyle)
            append(content)
        }
        return buff.toString()
    }

    private fun getColorFromULong(color: ULong): String {
        if (color == ULong.MIN_VALUE) return "black"
        return java.lang.Long.toHexString(color.toLong()).substring(2, 8)
    }


}