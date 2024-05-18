package org.openedx.core.data.model.room

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.openedx.core.data.model.room.discovery.CertificateDb
import org.openedx.core.data.model.room.discovery.CourseAccessDetailsDb
import org.openedx.core.data.model.room.discovery.CoursewareAccessDb
import org.openedx.core.data.model.room.discovery.EnrollmentDetailsDB
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.utils.TimeUtils

@Entity(tableName = "course_structure_table")
data class CourseStructureEntity(
    @ColumnInfo("root")
    val root: String,
    @PrimaryKey
    @ColumnInfo("id")
    val id: String,
    @ColumnInfo("blocks")
    val blocks: List<BlockDb>,
    @ColumnInfo("name")
    val name: String,
    @ColumnInfo("number")
    val number: String,
    @ColumnInfo("org")
    val org: String,
    @ColumnInfo("start")
    val start: String?,
    @ColumnInfo("startDisplay")
    val startDisplay: String,
    @ColumnInfo("startType")
    val startType: String,
    @ColumnInfo("end")
    val end: String?,
    @Embedded
    val coursewareAccess: CoursewareAccessDb?,
    @Embedded
    val media: MediaDb?,
    @Embedded
    val courseAccessDetails: CourseAccessDetailsDb,
    @Embedded
    val certificate: CertificateDb?,
    @Embedded
    val enrollmentDetails: EnrollmentDetailsDB,
    @ColumnInfo("isSelfPaced")
    val isSelfPaced: Boolean
) {

    fun mapToDomain(): CourseStructure {
        return CourseStructure(
            root,
            blocks.map { it.mapToDomain(blocks) },
            id,
            name,
            number,
            org,
            TimeUtils.iso8601ToDate(start ?: ""),
            startDisplay,
            startType,
            TimeUtils.iso8601ToDate(end ?: ""),
            coursewareAccess?.mapToDomain(),
            media?.mapToDomain(),
            courseAccessDetails.mapToDomain(),
            certificate?.mapToDomain(),
            enrollmentDetails.mapToDomain(),
            isSelfPaced,
            null,
        )
    }

}