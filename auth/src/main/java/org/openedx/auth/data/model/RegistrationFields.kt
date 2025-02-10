package org.openedx.auth.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.RegistrationField
import org.openedx.core.domain.model.RegistrationFieldType

data class RegistrationFields(
    @SerializedName("fields")
    val fields: List<Field>?,
) {

    data class Field(
        @SerializedName("name")
        val name: String?,
        @SerializedName("label")
        val label: String?,
        @SerializedName("type")
        val type: String?,
        @SerializedName("placeholder")
        val placeholder: String?,
        @SerializedName("instructions")
        val instructions: String?,
        @SerializedName("exposed")
        val exposed: Boolean?,
        @SerializedName("required")
        val required: Boolean?,
        @SerializedName("restrictions")
        val restrictions: Restrictions?,
        @SerializedName("options")
        val options: List<Option>?
    ) {
        fun mapToDomain(): RegistrationField {
            return RegistrationField(
                name = name ?: "",
                label = label ?: "",
                type = RegistrationFieldType.returnLocalTypeFromServerType(type),
                placeholder = placeholder ?: "",
                instructions = instructions ?: "",
                exposed = exposed ?: false,
                required = required ?: false,
                restrictions = restrictions?.mapToDomain() ?: RegistrationField.Restrictions(),
                options = options?.map { it.mapToDomain() } ?: emptyList()
            )
        }
    }

    data class Restrictions(
        @SerializedName("max_length")
        val maxLength: Int?,
        @SerializedName("min_length")
        val minLength: Int?
    ) {
        fun mapToDomain(): RegistrationField.Restrictions {
            return RegistrationField.Restrictions(
                maxLength = maxLength ?: 128,
                minLength = minLength ?: 1
            )
        }
    }

    data class Option(
        @SerializedName("value")
        val value: String?,
        @SerializedName("name")
        val name: String?,
        @SerializedName("default")
        val default: String?
    ) {
        fun mapToDomain(): RegistrationField.Option {
            return RegistrationField.Option(
                value = value ?: "",
                name = name ?: "",
                default = default ?: ""
            )
        }
    }
}
