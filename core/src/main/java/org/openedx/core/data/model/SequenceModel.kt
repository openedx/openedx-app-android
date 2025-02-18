package org.openedx.core.data.model

import com.google.gson.annotations.SerializedName
import org.openedx.core.domain.model.Subsection

data class SequenceModel(
    @SerializedName("element_id")
    val elementId: String,
    @SerializedName("item_id")
    val itemId: String,
    @SerializedName("banner_text")
    val bannerText: String?,
    @SerializedName("gated_content")
    val gatedContentModel: GatedContentModel,
    @SerializedName("sequence_name")
    val sequenceName: String,
    @SerializedName("display_name")
    val displayName: String,
) {
    fun mapToDomain(): Subsection {
        return Subsection(
            elementId = elementId,
            itemId = itemId,
            bannerText = bannerText,
            subsectionName = sequenceName,
            displayName = displayName,
            gatedContent = gatedContentModel.mapToDomain(),
        )
    }
}
