package org.openedx.core.data.model

import android.text.TextUtils
import com.google.gson.annotations.SerializedName
import org.openedx.core.data.model.room.discovery.EnrolledCourseEntity
import org.openedx.core.domain.ProductInfo
import org.openedx.core.domain.model.EnrolledCourse
import org.openedx.core.domain.model.EnrollmentMode
import org.openedx.core.utils.TimeUtils

data class EnrolledCourse(
    @SerializedName("audit_access_expires")
    val auditAccessExpires: String?,
    @SerializedName("created")
    val created: String?,
    @SerializedName("mode")
    val mode: String?,
    @SerializedName("is_active")
    val isActive: Boolean?,
    @SerializedName("course")
    val course: EnrolledCourseData?,
    @SerializedName("certificate")
    val certificate: Certificate?,
    @SerializedName("course_modes")
    val courseModes: List<CourseMode>?,
) {
    fun mapToDomain(): EnrolledCourse {
        return EnrolledCourse(
            auditAccessExpires = TimeUtils.iso8601ToDate(auditAccessExpires ?: ""),
            created = created ?: "",
            mode = mode ?: "",
            isActive = isActive ?: false,
            course = course?.mapToDomain()!!,
            certificate = certificate?.mapToDomain(),
            productInfo = courseModes?.find {
                EnrollmentMode.VERIFIED.toString().equals(it.slug, ignoreCase = true)
            }?.takeIf {
                TextUtils.isEmpty(it.androidSku).not() && TextUtils.isEmpty(it.storeSku).not()
            }?.run {
                ProductInfo(courseSku = androidSku!!, storeSku = storeSku!!)
            }
        )
    }

    fun mapToRoomEntity(): EnrolledCourseEntity {
        return EnrolledCourseEntity(
            courseId = course?.id ?: "",
            auditAccessExpires = auditAccessExpires ?: "",
            created = created ?: "",
            mode = mode ?: "",
            isActive = isActive ?: false,
            course = course?.mapToRoomEntity()!!,
            certificate = certificate?.mapToRoomEntity()
        )
    }

    fun setStoreSku(storeProductPrefix: String) {
        courseModes?.forEach {
            it.setStoreProductSku(storeProductPrefix)
        }
    }
}
