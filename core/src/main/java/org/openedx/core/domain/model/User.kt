package org.openedx.core.domain.model

data class User(
    val id: Long,
    val username: String?,
    val email: String?,
    val name: String
)
