package org.openedx.core.data.model

import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.BlockDb
import org.openedx.core.data.model.room.CourseStructureEntity
import org.openedx.core.data.model.room.MediaDb
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.ProductInfo
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.EnrollmentMode
import org.openedx.core.utils.TimeUtils
import java.lang.reflect.Type

data class CourseStructureModel(
    @SerializedName("root")
    val root: String,
    @SerializedName("blocks")
    val blockData: Map<String, Block>,
    @SerializedName("id")
    var id: String?,
    @SerializedName("name")
    var name: String?,
    @SerializedName("number")
    var number: String?,
    @SerializedName("org")
    var org: String?,
    @SerializedName("start")
    var start: String?,
    @SerializedName("start_display")
    var startDisplay: String?,
    @SerializedName("start_type")
    var startType: String?,
    @SerializedName("end")
    var end: String?,
    @SerializedName("courseware_access")
    var coursewareAccess: CoursewareAccess?,
    @SerializedName("media")
    var media: Media?,
    @SerializedName("course_access_details")
    val courseAccessDetails: CourseAccessDetails,
    @SerializedName("certificate")
    val certificate: Certificate?,
    @SerializedName("enrollment_details")
    val enrollmentDetails: EnrollmentDetails,
    @SerializedName("is_self_paced")
    var isSelfPaced: Boolean?,
    @SerializedName("course_modes")
    val courseModes: List<CourseMode>?,
) {
    fun mapToDomain(): CourseStructure {
        return CourseStructure(
            root = root,
            blockData = blockData.map {
                it.value.mapToDomain(blockData)
            },
            id = id ?: "",
            name = name ?: "",
            number = number ?: "",
            org = org ?: "",
            start = TimeUtils.iso8601ToDate(start ?: ""),
            startDisplay = startDisplay ?: "",
            startType = startType ?: "",
            end = TimeUtils.iso8601ToDate(end ?: ""),
            coursewareAccess = coursewareAccess?.mapToDomain(),
            media = media?.mapToDomain(),
            courseAccessDetails = courseAccessDetails.mapToDomain(),
            certificate = certificate?.mapToDomain(),
            enrollmentDetails = enrollmentDetails.mapToDomain(),
            isSelfPaced = isSelfPaced ?: false,
            productInfo = courseModes?.find {
                EnrollmentMode.VERIFIED.toString().equals(it.slug, ignoreCase = true)
            }?.takeIf {
                TextUtils.isEmpty(it.androidSku).not() && TextUtils.isEmpty(it.storeSku).not()
            }?.run {
                ProductInfo(courseSku = androidSku!!, storeSku = storeSku!!)
            }
        )
    }

    fun mapToRoomEntity(): CourseStructureEntity {
        return CourseStructureEntity(
            root,
            blocks = blockData.map { BlockDb.createFrom(it.value) },
            id = id ?: "",
            name = name ?: "",
            number = number ?: "",
            org = org ?: "",
            start = start ?: "",
            startDisplay = startDisplay ?: "",
            startType = startType ?: "",
            end = end ?: "",
            coursewareAccess = coursewareAccess?.mapToRoomEntity(),
            media = MediaDb.createFrom(media),
            courseAccessDetails = courseAccessDetails.mapToRoomEntity(),
            certificate = certificate?.mapToRoomEntity(),
            enrollmentDetails = enrollmentDetails.mapToRoomEntity(),
            isSelfPaced = isSelfPaced ?: false
        )
    }

    class Deserializer(val corePreferences: CorePreferences) :
        JsonDeserializer<CourseStructureModel> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): CourseStructureModel {
            val courseStructure = Gson().fromJson(json, CourseStructureModel::class.java)
            if (corePreferences.appConfig.iapConfig.productPrefix.isNullOrEmpty().not()) {
                courseStructure.courseModes?.forEach { courseModes ->
                    courseModes.setStoreProductSku(corePreferences.appConfig.iapConfig.productPrefix!!)
                }
            }
            return courseStructure
        }
    }
}
