package com.raccoongang.dashboard.presentation

import androidx.fragment.app.FragmentManager
import com.raccoongang.core.domain.model.Certificate
import com.raccoongang.core.domain.model.CoursewareAccess
import java.util.*

interface DashboardRouter {

    fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        title: String,
        image: String,
        certificate: Certificate,
        coursewareAccess: CoursewareAccess,
        auditAccessExpires: Date?
    )

}