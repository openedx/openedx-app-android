package org.openedx.core.domain.model

import android.os.Parcelable
import com.google.gson.internal.bind.util.ISO8601Utils
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.room.discovery.EnrollmentDetailsDB
import java.util.Date

@Parcelize
data class EnrollmentDetails(
    val created: Date?,
    val mode: String?,
    val isActive: Boolean,
    val upgradeDeadline: Date?,
) : Parcelable {

    fun mapToEntity() = EnrollmentDetailsDB(
        created = created?.let { ISO8601Utils.format(it) },
        mode = mode,
        isActive = isActive,
        upgradeDeadline = upgradeDeadline?.let { ISO8601Utils.format(it) }
    )
}
