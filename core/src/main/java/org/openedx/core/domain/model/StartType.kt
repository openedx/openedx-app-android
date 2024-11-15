package org.openedx.core.domain.model

import com.google.gson.annotations.SerializedName

enum class StartType(val type: String) {
    /**
     * Course's start date is provided as an unformatted string
     */
    @SerializedName("string")
    STRING("string"),

    /**
     * Course's start date is provided as a date-formatted string
     */
    @SerializedName("timestamp")
    TIMESTAMP("timestamp"),

    /**
     * Course's start date is unset
     */
    @SerializedName("empty")
    EMPTY("empty")
}
