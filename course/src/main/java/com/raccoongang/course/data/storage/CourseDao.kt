package com.raccoongang.course.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.raccoongang.core.data.model.room.CourseEntity
import com.raccoongang.core.data.model.room.CourseStructureEntity
import com.raccoongang.core.data.model.room.discovery.EnrolledCourseEntity

@Dao
interface CourseDao {

    @Query("SELECT * FROM course_discovery_table WHERE id=:id")
    suspend fun getCourseById(id: String): CourseEntity?

    @Query("SELECT * FROM course_enrolled_table WHERE id=:id")
    suspend fun getEnrolledCourseById(id: String): EnrolledCourseEntity?

    @Query("SELECT * FROM course_structure_table WHERE id=:id")
    suspend fun getCourseStructureById(id: String): CourseStructureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseEntity(vararg courseEntity: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourseStructureEntity(vararg courseStructureEntity: CourseStructureEntity)
}
