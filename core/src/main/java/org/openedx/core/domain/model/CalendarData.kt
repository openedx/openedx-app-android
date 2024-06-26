package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CalendarData(
    val title: String,
    val color: Int
) : Parcelable
