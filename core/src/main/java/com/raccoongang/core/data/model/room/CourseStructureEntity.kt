package com.raccoongang.core.data.model.room

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.raccoongang.core.data.model.room.discovery.CertificateDb
import com.raccoongang.core.data.model.room.discovery.CoursewareAccessDb
import com.raccoongang.core.domain.model.Block
import com.raccoongang.core.domain.model.CourseStructure
import com.raccoongang.core.utils.TimeUtils

@Entity(tableName = "course_structure_table")
data class CourseStructureEntity(
    val root: String,
    @PrimaryKey
    val id: String,
    val name: String,
    val number: String,
    val org: String,
    val start: String?,
    val startDisplay: String,
    val startType: String,
    val end: String?,
    @Embedded
    val coursewareAccess: CoursewareAccessDb,
    @Embedded
    val media: MediaDb?,
    @Embedded
    val certificate: CertificateDb?,
    val isSelfPaced: Boolean
) {

    fun mapToDomain(blocks: List<Block>): CourseStructure {
        return CourseStructure(
            root,
            blocks,
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