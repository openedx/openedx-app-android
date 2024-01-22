package org.openedx.core.interfaces

interface EnrollInCourseInteractor {
    suspend fun enrollInACourse(id: String)
}
