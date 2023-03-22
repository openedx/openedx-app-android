package com.raccoongang.core.system.notifier

data class CourseVideoPositionChanged(
    val videoUrl: String,
    val videoTime: Long
) : CourseEvent