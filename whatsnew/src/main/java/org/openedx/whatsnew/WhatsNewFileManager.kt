package org.openedx.whatsnew

import android.content.Context
import com.google.gson.Gson
import org.openedx.whatsnew.data.model.WhatsNewItem

class WhatsNewFileManager(private val context: Context) {
    fun getNewestData(): org.openedx.whatsnew.domain.model.WhatsNewItem {
        val jsonString = context.resources.openRawResource(R.raw.whats_new)
            .bufferedReader()
            .use { it.readText() }
        val whatsNewListData = Gson().fromJson(jsonString, Array<WhatsNewItem>::class.java)
        return whatsNewListData[0].mapToDomain(context)
    }
}