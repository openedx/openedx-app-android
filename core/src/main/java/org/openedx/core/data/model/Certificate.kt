package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.discovery.CertificateDb
import org.openedx.core.domain.model.Certificate

data class Certificate(
    @SerializedName("url")
    val certificateURL: String?
) {
    fun mapToDomain(): Certificate {
        return Certificate(
            certificateURL = certificateURL
        )
    }

    fun mapToRoomEntity() = CertificateDb(certificateURL)
}
