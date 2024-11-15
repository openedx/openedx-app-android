package org.openedx.course.presentation.handouts

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.config.Config
import org.openedx.core.domain.model.AnnouncementModel
import org.openedx.core.domain.model.HandoutsModel
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseAnalyticsEvent
import org.openedx.course.presentation.CourseAnalyticsKey
import org.openedx.foundation.presentation.BaseViewModel

class HandoutsViewModel(
    private val courseId: String,
    val handoutsType: String,
    private val config: Config,
    private val interactor: CourseInteractor,
    private val courseAnalytics: CourseAnalytics,
) : BaseViewModel() {

    val apiHostUrl get() = config.getApiHostURL()

    private val _uiState = MutableStateFlow<HandoutsUIState>(HandoutsUIState.Loading)
    val uiState: StateFlow<HandoutsUIState>
        get() = _uiState.asStateFlow()

    init {
        getCourseHandouts()
    }

    private fun getCourseHandouts() {
        viewModelScope.launch {
            var emptyState = false
            try {
                if (HandoutsType.valueOf(handoutsType) == HandoutsType.Handouts) {
                    val handouts = interactor.getHandouts(courseId)
                    if (handouts.handoutsHtml.isNotBlank()) {
                        _uiState.value = HandoutsUIState.HTMLContent(handoutsToHtml(handouts))
                    } else {
                        emptyState = true
                    }
                } else {
                    val announcements = interactor.getAnnouncements(courseId)
                    if (announcements.isNotEmpty()) {
                        _uiState.value =
                            HandoutsUIState.HTMLContent(announcementsToHtml(announcements))
                    } else {
                        emptyState = true
                    }
                }
            } catch (_: Exception) {
                // ignore e.printStackTrace()
                emptyState = true
            }
            if (emptyState) {
                _uiState.value = HandoutsUIState.Error
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
                " body {\n" +
                "   background-color: #${getColorFromULong(bgColor)};\n" +
                "   color: #${getColorFromULong(textColor)};\n" +
                " }\n" +
                "</style>"
        val buff = StringBuffer().apply {
            if (bgColor != ULong.MIN_VALUE) append(darkThemeStyle)
            append(content)
        }
        return buff.toString()
    }

    @Suppress("MagicNumber")
    private fun getColorFromULong(color: ULong): String {
        if (color == ULong.MIN_VALUE) return "black"
        return java.lang.Long.toHexString(color.toLong()).substring(2, 8)
    }

    fun logEvent(event: CourseAnalyticsEvent) {
        courseAnalytics.logScreenEvent(
            screenName = event.eventName,
            params = buildMap {
                put(CourseAnalyticsKey.NAME.key, event.biValue)
                put(CourseAnalyticsKey.COURSE_ID.key, courseId)
            }
        )
    }
}
