package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.openedx.core.extension.safeDivBy

@Parcelize
data class Progress(
    val completed: Int,
    val total: Int,
) : Parcelable {

    @IgnoredOnParcel
    val value: Float = completed.toFloat().safeDivBy(total.toFloat())

    companion object {
        val DEFAULT_PROGRESS = Progress(0, 0)
    }
}
