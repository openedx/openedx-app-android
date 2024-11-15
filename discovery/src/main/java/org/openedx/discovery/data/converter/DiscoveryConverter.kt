package org.openedx.discovery.data.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import org.openedx.core.data.model.room.BannerImageDb
import org.openedx.core.data.model.room.CourseImageDb
import org.openedx.core.data.model.room.CourseVideoDb
import org.openedx.core.data.model.room.ImageDb

class DiscoveryConverter {

    @TypeConverter
    fun fromImageDb(imageDb: ImageDb?): String {
        if (imageDb == null) return ""
        val json = Gson().toJson(imageDb)
        return json.toString()
    }

    @TypeConverter
    fun toImageDb(value: String): ImageDb? {
        if (value.isEmpty()) return null
        return Gson().fromJson(value, ImageDb::class.java)
    }

    @TypeConverter
    fun fromBannerImage(bannerImageDb: BannerImageDb?): String {
        if (bannerImageDb == null) return ""
        val json = Gson().toJson(bannerImageDb)
        return json.toString()
    }

    @TypeConverter
    fun toBannerImageDb(value: String): BannerImageDb? {
        if (value.isEmpty()) return null
        return Gson().fromJson(value, BannerImageDb::class.java)
    }

    @TypeConverter
    fun fromCourseImageDb(courseImageDb: CourseImageDb?): String {
        if (courseImageDb == null) return ""
        val json = Gson().toJson(courseImageDb)
        return json.toString()
    }

    @TypeConverter
    fun toCourseImageDb(value: String): CourseImageDb? {
        if (value.isEmpty()) return null
        return Gson().fromJson(value, CourseImageDb::class.java)
    }

    @TypeConverter
    fun fromCourseVideoDb(courseVideoDb: CourseVideoDb?): String {
        if (courseVideoDb == null) return ""
        val json = Gson().toJson(courseVideoDb)
        return json.toString()
    }

    @TypeConverter
    fun toCourseVideoDb(value: String): CourseVideoDb? {
        if (value.isEmpty()) return null
        return Gson().fromJson(value, CourseVideoDb::class.java)
    }
}
