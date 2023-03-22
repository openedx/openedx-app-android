package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.data.model.room.discovery.CertificateDb
import com.raccoongang.core.domain.model.Certificate

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