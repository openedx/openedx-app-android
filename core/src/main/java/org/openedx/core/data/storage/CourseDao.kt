package org.openedx.core.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.openedx.core.data.model.room.CourseEnrollmentDetailsEntity
import org.openedx.core.data.model.room.CourseProgressEntity
import org.openedx.core.data.model.room.CourseStructureEntity
import org.openedx.core.data.model.room.VideoProgressEntity

@Dao
interface CourseDao {

    @Query("SELECT * FROM course_structure_table WHERE id=:id")
    suspend fun getCourseStructureById(id: String): CourseStructureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseStructureEntity(vararg courseStructureEntity: CourseStructureEntity)

    @Transaction
    suspend fun clearCachedData() {
        clearCourseStructure()
        clearVideoProgress()
        clearEnrollmentCachedData()
        clearCourseProgressData()
    }

    @Query("DELETE FROM course_structure_table")
    suspend fun clearCourseStructure()

    @Query("DELETE FROM video_progress_table")
    suspend fun clearVideoProgress()

    @Query("DELETE FROM course_enrollment_details_table")
    suspend fun clearEnrollmentCachedData()

    @Query("DELETE FROM course_progress_table")
    suspend fun clearCourseProgressData()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseEnrollmentDetailsEntity(vararg courseEnrollmentDetailsEntity: CourseEnrollmentDetailsEntity)

    @Query("SELECT * FROM course_enrollment_details_table WHERE id=:id")
    suspend fun getCourseEnrollmentDetailsById(id: String): CourseEnrollmentDetailsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoProgressEntity(vararg videoProgressEntity: VideoProgressEntity)

    @Query("SELECT * FROM video_progress_table WHERE block_id=:blockId")
    suspend fun getVideoProgressByBlockId(blockId: String): VideoProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseProgressEntity(vararg courseProgressEntity: CourseProgressEntity)

    @Query("SELECT * FROM course_progress_table WHERE courseId=:id")
    suspend fun getCourseProgressById(id: String): CourseProgressEntity?
}
