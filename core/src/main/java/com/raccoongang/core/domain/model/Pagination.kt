package com.raccoongang.core.domain.model

data class Pagination(
    val count: Int,
    val next: String,
    val numPages: Int,
    val previous: String
)