package com.raccoongang.core.domain.model


data class User(
    val id: Long,
    val username: String?,
    val email: String?,
    val name: String
)