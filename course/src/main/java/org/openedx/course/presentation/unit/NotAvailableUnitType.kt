package org.openedx.course.presentation.unit

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class NotAvailableUnitType : Parcelable {
    MOBILE_UNSUPPORTED, OFFLINE_UNSUPPORTED, NOT_DOWNLOADED
}
