package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.data.model.room.discovery.CertificateDb

@Parcelize
data class Certificate(
    val certificateURL: String?
) : Parcelable {
    fun isCertificateEarned() = certificateURL?.isNotEmpty() == true

    fun mapToRoomEntity() = CertificateDb(certificateURL)
}
