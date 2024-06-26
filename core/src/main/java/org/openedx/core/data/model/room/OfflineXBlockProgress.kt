package org.openedx.core.data.model.room

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONObject

@Entity(tableName = "offline_x_block_progress_table")
data class OfflineXBlockProgress(
    @PrimaryKey
    @ColumnInfo("id")
    val blockId: String,
    @ColumnInfo("courseId")
    val courseId: String,
    @Embedded
    val jsonProgress: XBlockProgressData,
)

data class XBlockProgressData(
    @PrimaryKey
    @ColumnInfo("url")
    val url: String,
    @ColumnInfo("type")
    val type: String,
    @ColumnInfo("data")
    val data: String
) {

    fun toJson(): String {
        val jsonObject = JSONObject()
        jsonObject.put("url", url)
        jsonObject.put("type", type)
        jsonObject.put("data", data)

        return jsonObject.toString()
    }

    companion object {
        fun parseJson(jsonString: String): XBlockProgressData {
            val jsonObject = JSONObject(jsonString)
            val url = jsonObject.getString("url")
            val type = jsonObject.getString("type")
            val data = jsonObject.getString("data")

            return XBlockProgressData(url, type, data)
        }
    }
}
