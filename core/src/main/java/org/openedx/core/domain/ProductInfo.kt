package org.openedx.core.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProductInfo(
    val courseSku: String,
    val storeSku: String,
) : Parcelable
