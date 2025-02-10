package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class EnrollmentDetails(
    val created: Date?,
    val mode: String?,
    val isActive: Boolean,
    val upgradeDeadline: Date?,
) : Parcelable
