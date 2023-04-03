package com.raccoongang.course.data.storage

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.raccoongang.core.data.model.room.BlockDb
import com.raccoongang.core.data.model.room.VideoInfoDb
import com.raccoongang.core.extension.genericType

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

}
