package org.openedx.auth.data.model

import com.google.gson.annotations.SerializedName

data class ValidationFields(
    @SerializedName("validation_decisions")
    val validationResult: Map<String, String>
) {
    fun hasValidationError() = validationResult.values.any { it != "" }
}
