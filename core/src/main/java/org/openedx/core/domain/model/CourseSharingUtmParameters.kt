package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.room.discovery.CourseSharingUtmParametersDb

@Parcelize
data class CourseSharingUtmParameters(
    val facebook: String,
    val twitter: String
) : Parcelable {

    fun mapToEntity() = CourseSharingUtmParametersDb(
        facebook = facebook,
        twitter = twitter
    )
}
