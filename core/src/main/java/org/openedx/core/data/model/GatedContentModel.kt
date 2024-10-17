package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.GatedContent

data class GatedContentModel(
    @SerializedName("prereq_id")
    val prereqId: String?,
    @SerializedName("prereq_url")
    val prereqUrl: String?,
    @SerializedName("prereq_section_name")
    val prereqSectionName: String?,
    @SerializedName("gated")
    val gated: Boolean,
    @SerializedName("gated_section_name")
    val gatedSectionName: String?,
) {
    fun mapToDomain(): GatedContent {
        return GatedContent(
            prereqId = prereqId,
            prereqUrl = prereqUrl,
            prereqSubsectionName = prereqSectionName,
            gated = gated,
            gatedSubsectionName = gatedSectionName
        )
    }
}
