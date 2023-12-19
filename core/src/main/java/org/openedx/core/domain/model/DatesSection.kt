package org.openedx.core.domain.model

enum class DatesSection(val key:String) {
    COMPLETED("Completed"),
    PAST_DUE("Past Due"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    NEXT_WEEK("Next Week"),
    UPCOMING("Upcoming"),
    NONE("none");
}
