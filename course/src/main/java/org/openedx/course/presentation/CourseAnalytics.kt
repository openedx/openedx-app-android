package org.openedx.course.presentation

interface CourseAnalytics {
    fun courseEnrollClickedEvent(courseId: String, courseName: String)
    fun courseEnrollSuccessEvent(courseId: String, courseName: String)
    fun viewCourseClickedEvent(courseId: String, courseName: String)
    fun resumeCourseTappedEvent(courseId: String, courseName: String, blockId: String)
    fun sequentialClickedEvent(courseId: String, courseName: String, blockId: String, blockName: String)
    fun verticalClickedEvent(courseId: String, courseName: String, blockId: String, blockName: String)
    fun nextBlockClickedEvent(courseId: String, courseName: String, blockId: String, blockName: String)
    fun prevBlockClickedEvent(courseId: String, courseName: String, blockId: String, blockName: String)
    fun finishVerticalClickedEvent(courseId: String, courseName: String, blockId: String, blockName: String)
    fun finishVerticalNextClickedEvent(courseId: String, courseName: String, blockId: String, blockName: String)
    fun finishVerticalBackClickedEvent(courseId: String, courseName: String)
    fun courseTabClickedEvent(courseId: String, courseName: String)
    fun videoTabClickedEvent(courseId: String, courseName: String)
    fun discussionTabClickedEvent(courseId: String, courseName: String)
    fun datesTabClickedEvent(courseId: String, courseName: String)
    fun handoutsTabClickedEvent(courseId: String, courseName: String)
}
