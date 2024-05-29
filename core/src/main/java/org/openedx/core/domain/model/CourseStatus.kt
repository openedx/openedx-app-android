package org.openedx.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CourseStatus(
    val lastVisitedModuleId: String,
    val lastVisitedModulePath: List<String>,
    val lastVisitedBlockId: String,
    val lastVisitedUnitDisplayName: String,
) : Parcelable
