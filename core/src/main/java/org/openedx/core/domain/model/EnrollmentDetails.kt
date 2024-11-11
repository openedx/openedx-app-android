package org.openedx.core.domain.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.EnrollmentDetails
import org.openedx.core.extension.isNotNull
import java.util.Date

@Parcelize
data class EnrollmentDetails(
    val created: Date?,
    val mode: String?,
    val isActive: Boolean,
    val upgradeDeadline: Date?,
) : Parcelable

