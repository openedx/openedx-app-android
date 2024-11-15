package org.openedx.profile.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.LanguageProficiency

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
