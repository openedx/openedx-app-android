package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName

data class ErrorResponse(
    @SerializedName("error", alternate = ["error_code"])
    val error: String?,
    @SerializedName("error_description", alternate = ["value", "developer_message"])
    val errorDescription: String?
)
