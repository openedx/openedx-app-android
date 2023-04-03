package com.raccoongang.core.data.model.room

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.raccoongang.core.data.model.room.discovery.CertificateDb
import com.raccoongang.core.data.model.room.discovery.CoursewareAccessDb
import com.raccoongang.core.domain.model.CourseStructure
import com.raccoongang.core.utils.TimeUtils

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
    val coursewareAccess: CoursewareAccessDb,
    @Embedded
    val media: MediaDb?,
    @Embedded
    val certificate: CertificateDb?,
    @ColumnInfo("isSelfPaced")
    val isSelfPaced: Boolean
) {

    fun mapToDomain(): CourseStructure {
        return CourseStructure(
            root,
            blocks.map { it.mapToDomain() },
            id,
            name,
            number,
            org,
            TimeUtils.iso8601ToDate(start ?: ""),
            startDisplay,
            startType,
            TimeUtils.iso8601ToDate(end ?: ""),
            coursewareAccess.mapToDomain(),
            media?.mapToDomain(),
            certificate?.mapToDomain(),
            isSelfPaced
        )
    }

}