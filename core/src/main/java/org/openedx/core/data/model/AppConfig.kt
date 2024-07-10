package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.AppConfig as DomainAppConfig

data class AppConfig(
    @SerializedName("course_dates_calendar_sync")
    val calendarSyncConfig: CalendarSyncConfig = CalendarSyncConfig(),

    @SerializedName("value_prop_enabled")
    val isValuePropEnabled: Boolean = false,

    @SerializedName("iap_config")
    val iapConfig: IAPConfig = IAPConfig(),
) {
    fun mapToDomain(): DomainAppConfig {
        return DomainAppConfig(
            courseDatesCalendarSync = calendarSyncConfig.mapToDomain(),
            isValuePropEnabled = isValuePropEnabled,
            iapConfig = iapConfig.mapToDomain(),
        )
    }
}
