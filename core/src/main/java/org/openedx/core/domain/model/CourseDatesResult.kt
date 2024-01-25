package org.openedx.core.domain.model

data class CourseDatesResult(
    val datesSection: LinkedHashMap<DatesSection, List<CourseDateBlock>>,
    val courseBanner: CourseDatesBannerInfo,
)
