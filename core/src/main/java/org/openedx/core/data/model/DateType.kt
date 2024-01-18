package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.R

enum class DateType(val drawableResId: Int? = null) {
    @SerializedName("todays-date")
    TODAY_DATE,

    @SerializedName("course-start-date")
    COURSE_START_DATE(R.drawable.core_ic_start_end),

    @SerializedName("course-end-date")
    COURSE_END_DATE(R.drawable.core_ic_start_end),

    @SerializedName("course-expired-date")
    COURSE_EXPIRED_DATE(R.drawable.core_ic_course_expire),

    @SerializedName("assignment-due-date")
    ASSIGNMENT_DUE_DATE(R.drawable.core_ic_assignment),

    @SerializedName("certificate-available-date")
    CERTIFICATE_AVAILABLE_DATE(R.drawable.core_ic_certificate),

    @SerializedName("verified-upgrade-deadline")
    VERIFIED_UPGRADE_DEADLINE(R.drawable.core_ic_lock),

    @SerializedName("verification-deadline-date")
    VERIFICATION_DEADLINE_DATE(R.drawable.core_ic_lock),

    NONE,
}
