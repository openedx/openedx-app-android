@file:Suppress("DEPRECATION")

package org.openedx.course.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import androidx.annotation.DrawableRes
import coil.ImageLoader
import coil.request.ImageRequest

class ImageProcessor(private val context: Context) {
    fun loadImage(
        @DrawableRes
        defaultImage: Int,
        imageUrl: String,
        onComplete: (result: Drawable) -> Unit
    ) {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .target { result ->
                onComplete(result)
            }
            .error(defaultImage)
            .placeholder(defaultImage)
            .allowHardware(false)
            .build()
        loader.enqueue(request)
    }

    fun applyBlur(
        bitmap: Bitmap,
        blurRadio: Float
    ): Bitmap {
        val renderScript = RenderScript.create(context)
        val bitmapAlloc = Allocation.createFromBitmap(renderScript, bitmap)
        ScriptIntrinsicBlur.create(renderScript, bitmapAlloc.element).apply {
            setRadius(blurRadio)
            setInput(bitmapAlloc)
            repeat(times = 3) {
                forEach(bitmapAlloc)
            }
        }
        val newBitmap: Bitmap = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            Bitmap.Config.ARGB_8888
        )
        bitmapAlloc.copyTo(newBitmap)
        renderScript.destroy()
        return newBitmap
    }
}
