package org.openedx.profile.presentation.calendar

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class NewCalendarDialogType : Parcelable {
    CREATE_NEW, UPDATE
}
