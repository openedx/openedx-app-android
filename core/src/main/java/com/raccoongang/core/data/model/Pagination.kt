package com.raccoongang.core.data.model

import com.google.gson.annotations.SerializedName
import com.raccoongang.core.domain.model.Pagination as domainPagination

data class Pagination(
    @SerializedName("count")
    val count: Int?,
    @SerializedName("next")
    val next: String?,
    @SerializedName("num_pages")
    val numPages: Int?,
    @SerializedName("previous")
    val previous: String?,
) {
    fun mapToDomain() = domainPagination(
        count = count ?: 0,
        next = next ?: "",
        numPages = numPages ?: 0,
        previous = previous ?: ""
    )
}