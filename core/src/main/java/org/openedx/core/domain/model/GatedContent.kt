package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GatedContent(
    val prereqId: String?,
    val prereqUrl: String?,
    val prereqSubsectionName: String?,
    val gated: Boolean,
    val gatedSubsectionName: String?
) : Parcelable
