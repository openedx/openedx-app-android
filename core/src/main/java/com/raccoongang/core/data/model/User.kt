package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.domain.model.User

data class User(
    @SerializedName("id")
    val id: Long,
    @SerializedName("username")
    val username: String?,
    @SerializedName("email")
    val email: String?,
    @SerializedName("name")
    val name: String?
) {
    fun mapToDomain(): User {
        return User(
            id, username, email, name?:""
        )
    }
}
