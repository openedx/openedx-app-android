package org.openedx.dates.domain.interactor

import org.openedx.dates.data.repository.DatesRepository

class DatesInteractor(
    private val repository: DatesRepository
) {

    suspend fun getUserDates() = repository.getUserDates()

}
