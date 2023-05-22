package com.raccoongang.course.data.storage

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
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

    @Update
    suspend fun updateCourseEntity(courseEntity: CourseEntity)

    @Update
    suspend fun updateCourseStructureEntity(courseStructureEntity: CourseStructureEntity)
}
