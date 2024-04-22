package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.openedx.core.ApiConstants

data class RegistrationField(
    val name: String,
    val label: String,
    val type: RegistrationFieldType,
    val placeholder: String,
    val instructions: String,
    val exposed: Boolean,
    val required: Boolean,
    val restrictions: Restrictions,
    val options: List<Option>,
    val errorInstructions: String = "",
    val defaultValue: Boolean = false,
) {

    data class Restrictions(
        val maxLength: Int = 128,
        val minLength: Int = 1,
    )

    @Parcelize
    data class Option(
        val value: String,
        val name: String,
        val default: String,
    ) : Parcelable
}

fun String.createHonorCodeField() = RegistrationField(
    name = ApiConstants.RegistrationFields.HONOR_CODE,
    label = this,
    type = RegistrationFieldType.PLAINTEXT,
    placeholder = "",
    instructions = "",
    exposed = false,
    required = false,
    restrictions = RegistrationField.Restrictions(),
    options = emptyList(),
    errorInstructions = ""
)

enum class RegistrationFieldType {
    TEXT, EMAIL, CONFIRM_EMAIL, PASSWORD, SELECT, TEXTAREA, CHECKBOX, PLAINTEXT, UNKNOWN;

    companion object {
        fun returnLocalTypeFromServerType(type: String?): RegistrationFieldType {
            return when (type) {
                "text" -> TEXT
                "email" -> EMAIL
                "confirm_email" -> CONFIRM_EMAIL
                "password" -> PASSWORD
                "select" -> SELECT
                "textarea" -> TEXTAREA
                "checkbox" -> CHECKBOX
                "plaintext" -> PLAINTEXT
                else -> UNKNOWN
            }
        }
    }
}
