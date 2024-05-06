package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.CourseMode
import kotlin.math.ceil

data class CourseMode(
    @SerializedName("slug")
    val slug: String?,

    @SerializedName("sku")
    val sku: String?,

    @SerializedName("android_sku")
    val androidSku: String?,

    @SerializedName("min_price")
    val price: Double?,

    var storeSku: String?,
) {
    fun setStoreProductSku(storeProductPrefix: String) {
        val ceilPrice = price
            ?.let { ceil(it).toInt() }
            ?.takeIf { it > 0 }

        if (storeProductPrefix.isNotBlank() && ceilPrice != null) {
            storeSku = "$storeProductPrefix$ceilPrice"
        }
    }

    fun mapToDomain(): CourseMode {
        return CourseMode(
            slug = slug,
            androidSku = androidSku,
            storeSku = storeSku
        )
    }
}
