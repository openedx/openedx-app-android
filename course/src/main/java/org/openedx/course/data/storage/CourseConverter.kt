package org.openedx.course.data.storage

import androidx.room.TypeConverter
import com.google.gson.Gson
import org.openedx.core.data.model.room.BlockDb
import org.openedx.core.data.model.room.VideoInfoDb
import org.openedx.core.data.model.room.discovery.CourseDateBlockDb
import org.openedx.foundation.extension.genericType

class CourseConverter {

    @TypeConverter
    fun fromVideoDb(value: VideoInfoDb?): String {
        if (value == null) return ""
        val json = Gson().toJson(value)
        return json.toString()
    }

    @TypeConverter
    fun toVideoDb(value: String): VideoInfoDb? {
        if (value.isEmpty()) return null
        return Gson().fromJson(value, VideoInfoDb::class.java)
    }

    @TypeConverter
    fun fromListOfString(value: List<String>): String {
        val json = Gson().toJson(value)
        return json.toString()
    }

    @TypeConverter
    fun toListOfString(value: String): List<String> {
        val type = genericType<List<String>>()
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    fun fromListOfBlockDbEntity(value: List<BlockDb>): String {
        val json = Gson().toJson(value)
        return json.toString()
    }

    @TypeConverter
    fun toListOfBlockDbEntity(value: String): List<BlockDb> {
        val type = genericType<List<BlockDb>>()
        return Gson().fromJson(value, type)
    }

    @TypeConverter
    fun fromStringToMap(value: String?): Map<String, String> {
        val mapType = genericType<HashMap<String, String>>()
        return Gson().fromJson(value, mapType)
    }

    @TypeConverter
    fun fromMapToString(map: Map<String, String>): String {
        val gson = Gson()
        return gson.toJson(map)
    }

    @TypeConverter
    fun fromListOfCourseDateBlockDb(value: List<CourseDateBlockDb>): String {
        val json = Gson().toJson(value)
        return json.toString()
    }

    @TypeConverter
    fun toListOfCourseDateBlockDb(value: String): List<CourseDateBlockDb> {
        val type = genericType<List<CourseDateBlockDb>>()
        return Gson().fromJson(value, type)
    }
}
