package org.openedx.course.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.openedx.core.data.model.room.CourseStructureEntity

@Dao
interface CourseDao {

    @Query("SELECT * FROM course_structure_table WHERE id=:id")
    suspend fun getCourseStructureById(id: String): CourseStructureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseStructureEntity(vararg courseStructureEntity: CourseStructureEntity)

    @Query("DELETE FROM course_structure_table")
    suspend fun clearCachedData()
}
