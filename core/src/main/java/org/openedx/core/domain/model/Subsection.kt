package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Subsection(
    val elementId: String,
    val itemId: String,
    val bannerText: String?,
    val gatedContent: GatedContent,
    val subsectionName: String,
    val displayName: String
) : Parcelable
