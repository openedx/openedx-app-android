package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName

enum class DateType {
    @SerializedName("todays-date")
    TODAY_DATE,

    @SerializedName("course-start-date")
    COURSE_START_DATE,

    @SerializedName("course-end-date")
    COURSE_END_DATE,

    @SerializedName("course-expired-date")
    COURSE_EXPIRED_DATE,

    @SerializedName("assignment-due-date")
    ASSIGNMENT_DUE_DATE,

    @SerializedName("certificate-available-date")
    CERTIFICATE_AVAILABLE_DATE,

    @SerializedName("verified-upgrade-deadline")
    VERIFIED_UPGRADE_DEADLINE,

    @SerializedName("verification-deadline-date")
    VERIFICATION_DEADLINE_DATE,

    NONE,
}
