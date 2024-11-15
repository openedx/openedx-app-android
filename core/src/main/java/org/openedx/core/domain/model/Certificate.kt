package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Certificate(
    val certificateURL: String?
) : Parcelable {
    fun isCertificateEarned() = certificateURL?.isNotEmpty() == true
}
