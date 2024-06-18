package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.CourseMode as CourseMode

data class CourseMode(
    @SerializedName("slug")
    val slug: String,
    @SerializedName("sku")
    val sku: String?,
    @SerializedName("android_sku")
    val androidSku: String?,
    @SerializedName("ios_sku")
    val iosSku: String?,
    @SerializedName("min_price")
    val minPrice: Int,
) {
    fun mapToDomain() = CourseMode(
        slug = slug,
        sku = sku,
        androidSku = androidSku,
        iosSku = iosSku,
        minPrice = minPrice,
    )
}
