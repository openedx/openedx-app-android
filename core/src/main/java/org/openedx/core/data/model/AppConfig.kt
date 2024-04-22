package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.AppConfig as DomainAppConfig

data class AppConfig(
    @SerializedName("course_dates_calendar_sync")
    val calendarSyncConfig: CalendarSyncConfig = CalendarSyncConfig(),
) {
    fun mapToDomain(): DomainAppConfig {
        return DomainAppConfig(
            courseDatesCalendarSync = calendarSyncConfig.mapToDomain(),
        )
    }
}
