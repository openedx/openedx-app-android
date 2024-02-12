package org.openedx.whatsnew.data.model

import android.content.Context
import com.google.gson.annotations.SerializedName

data class WhatsNewMessage(
    @SerializedName("image")
    val image: String,
    @SerializedName("title")
    val title: String,
    @SerializedName("message")
    val message: String
) {
    fun mapToDomain(context: Context) = org.openedx.whatsnew.domain.model.WhatsNewMessage(
        image = getDrawableIntFromString(context, image),
        title = title,
        message = message
    )

    private fun getDrawableIntFromString(context: Context, imageName: String): Int {
        val imageInt = context.resources.getIdentifier(imageName, "drawable", context.packageName)
        return if (imageInt == 0) {
            org.openedx.core.R.drawable.core_no_image_course
        } else {
            imageInt
        }
    }
}
