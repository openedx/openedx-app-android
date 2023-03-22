package com.raccoongang.profile.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.domain.model.LanguageProficiency

data class LanguageProficiency(
    @SerializedName("code")
    val code: String?
) {
    fun mapToDomain(): LanguageProficiency {
        return LanguageProficiency(
            code = code ?: ""
        )
    }
}