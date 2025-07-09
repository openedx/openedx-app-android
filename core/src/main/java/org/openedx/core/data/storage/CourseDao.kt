package org.openedx.core.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.openedx.core.data.model.room.CourseEnrollmentDetailsEntity
import org.openedx.core.data.model.room.CourseProgressEntity
import org.openedx.core.data.model.room.CourseStructureEntity

@Dao
interface CourseDao {

    @Query("SELECT * FROM course_structure_table WHERE id=:id")
    suspend fun getCourseStructureById(id: String): CourseStructureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseStructureEntity(vararg courseStructureEntity: CourseStructureEntity)

    @Transaction
    suspend fun clearCourseData() {
        clearCourseStructureData()
        clearCourseProgressData()
        clearEnrollmentCachedData()
    }

    @Query("DELETE FROM course_structure_table")
    suspend fun clearCourseStructureData()

    @Query("DELETE FROM course_progress_table")
    suspend fun clearCourseProgressData()

    @Query("DELETE FROM course_enrollment_details_table")
    suspend fun clearEnrollmentCachedData()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseEnrollmentDetailsEntity(vararg courseEnrollmentDetailsEntity: CourseEnrollmentDetailsEntity)

    @Query("SELECT * FROM course_enrollment_details_table WHERE id=:id")
    suspend fun getCourseEnrollmentDetailsById(id: String): CourseEnrollmentDetailsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseProgressEntity(vararg courseProgressEntity: CourseProgressEntity)

    @Query("SELECT * FROM course_progress_table WHERE courseId=:id")
    suspend fun getCourseProgressById(id: String): CourseProgressEntity?
}
