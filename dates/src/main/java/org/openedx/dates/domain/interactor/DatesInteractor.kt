package org.openedx.dates.domain.interactor

import org.openedx.dates.data.repository.DatesRepository

class DatesInteractor(
    private val repository: DatesRepository
) {

    suspend fun getUserDates(page: Int) = repository.getUserDates(page)

    suspend fun getUserDatesFromCache() = repository.getUserDatesFromCache()

    suspend fun preloadFirstPageCachedDates() = repository.preloadFirstPageCachedDates()

    suspend fun shiftAllDueDates() = repository.shiftAllDueDates()
}
