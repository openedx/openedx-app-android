package org.openedx.core.presentation.global

import org.openedx.core.R

enum class ErrorType(
    val iconResId: Int = 0,
    val titleResId: Int = 0,
    val descriptionResId: Int = 0,
    val actionResId: Int = 0,
) {
    CONNECTION_ERROR(
        iconResId = R.drawable.core_no_internet_connection,
        titleResId = R.string.core_no_internet_connection,
        descriptionResId = R.string.core_no_internet_connection_description,
        actionResId = R.string.core_reload,
    ),
    UNKNOWN_ERROR(
        iconResId = R.drawable.core_ic_unknown_error,
        titleResId = R.string.core_try_again,
        descriptionResId = R.string.core_something_went_wrong_description,
        actionResId = R.string.core_reload,
    ),
}
