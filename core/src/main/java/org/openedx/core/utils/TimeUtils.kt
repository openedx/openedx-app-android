package org.openedx.core.utils

import android.content.Context
import android.text.format.DateUtils
import com.google.gson.internal.bind.util.ISO8601Utils
import org.openedx.core.R
import org.openedx.core.domain.model.StartType
import org.openedx.core.system.ResourceManager
import java.text.ParseException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

object TimeUtils {

    private const val FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    private const val FORMAT_ISO_8601_WITH_TIME_ZONE = "yyyy-MM-dd'T'HH:mm:ssXXX"

    private const val SEVEN_DAYS_IN_MILLIS = 604800000L

    fun getCurrentTime(): Long {
        return Calendar.getInstance().timeInMillis
    }

    fun iso8601ToDate(text: String): Date? {
        return try {
            val parsePosition = ParsePosition(0)
            return ISO8601Utils.parse(text, parsePosition)
        } catch (e: ParseException) {
            null
        }
    }

    fun iso8601WithTimeZoneToDate(text: String): Date? {
        return try {
            val sdf = SimpleDateFormat(FORMAT_ISO_8601_WITH_TIME_ZONE, Locale.getDefault())
            sdf.parse(text)
        } catch (e: ParseException) {
            null
        }
    }

    fun iso8601ToDateWithTime(context: Context, text: String): String {
        return try {
            val courseDateFormat = SimpleDateFormat(FORMAT_ISO_8601, Locale.getDefault())
            val applicationDateFormat = SimpleDateFormat(
                context.getString(R.string.core_full_date_with_time), Locale.getDefault()
            )
            applicationDateFormat.format(courseDateFormat.parse(text)!!)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun dateToCourseDate(resourceManager: ResourceManager, date: Date?): String {
        return formatDate(
            format = resourceManager.getString(R.string.core_date_format_MMM_dd_yyyy), date = date
        )
    }

    private fun formatDate(format: String, date: Date?): String {
        if (date == null) {
            return ""
        }
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        return sdf.format(date)
    }

    /**
     * Checks if the given date is past today.
     *
     * @param today     Today's date.
     * @param otherDate Other date to cross-match with today's date.
     * @return <code>true</code> if the other date is past today,
     * <code>false</code> otherwise.
     */
    private fun isDatePassed(today: Date, otherDate: Date?): Boolean {
        return otherDate != null && today.after(otherDate)
    }

    fun getCourseFormattedDate(
        context: Context,
        today: Date,
        expiry: Date?,
        start: Date?,
        end: Date?,
        startType: String,
        startDisplay: String
    ): String {
        val formattedDate: String
        val resourceManager = ResourceManager(context)

        if (isDatePassed(today, start)) {
            if (expiry != null) {
                val dayDifferenceInMillis = if (today.after(expiry)) {
                    today.time - expiry.time
                } else {
                    expiry.time - today.time
                }

                if (isDatePassed(today, expiry)) {
                    formattedDate = if (dayDifferenceInMillis > SEVEN_DAYS_IN_MILLIS) {
                        resourceManager.getString(
                            R.string.core_label_expired_on,
                            dateToCourseDate(resourceManager, expiry)
                        )
                    } else {
                        val timeSpan = DateUtils.getRelativeTimeSpanString(
                            expiry.time,
                            today.time,
                            DateUtils.SECOND_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE
                        ).toString()
                        resourceManager.getString(R.string.core_label_expired, timeSpan)
                    }
                } else {
                    formattedDate = if (dayDifferenceInMillis > SEVEN_DAYS_IN_MILLIS) {
                        resourceManager.getString(
                            R.string.core_label_expires_on,
                            dateToCourseDate(resourceManager, expiry)
                        )
                    } else {
                        val timeSpan = DateUtils.getRelativeTimeSpanString(
                            expiry.time,
                            today.time,
                            DateUtils.SECOND_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE
                        ).toString()
                        resourceManager.getString(R.string.core_label_expires, timeSpan)
                    }
                }
            } else {
                formattedDate = if (end == null) {
                    if (startType == StartType.TIMESTAMP.type && start != null) {
                        resourceManager.getString(
                            R.string.core_label_starting, dateToCourseDate(resourceManager, start)
                        )
                    } else if (startType == StartType.STRING.type && start != null) {
                        resourceManager.getString(R.string.core_label_starting, startDisplay)
                    } else {
                        val soon = resourceManager.getString(R.string.core_assessment_soon)
                        resourceManager.getString(R.string.core_label_starting, soon)
                    }
                } else if (isDatePassed(today, end)) {
                    resourceManager.getString(
                        R.string.core_label_ended, dateToCourseDate(resourceManager, end)
                    )
                } else {
                    resourceManager.getString(
                        R.string.core_label_ends, dateToCourseDate(resourceManager, end)
                    )
                }
            }
        } else {
            formattedDate = if (startType == StartType.TIMESTAMP.type && start != null) {
                resourceManager.getString(
                    R.string.core_label_starting, dateToCourseDate(resourceManager, start)
                )
            } else if (startType == StartType.STRING.type && start != null) {
                resourceManager.getString(R.string.core_label_starting, startDisplay)
            } else {
                val soon = resourceManager.getString(R.string.core_assessment_soon)
                resourceManager.getString(R.string.core_label_starting, soon)
            }
        }
        return formattedDate
    }

    /**
     * Method to get the formatted time string in terms of relative time with minimum resolution of minutes.
     * For example, if the time difference is 1 minute, it will return "1m ago".
     *
     * @param date Date object to be formatted.
     */
    fun getFormattedTime(date: Date): String {
        return DateUtils.getRelativeTimeSpanString(
            date.time,
            getCurrentTime(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_TIME
        ).toString()
    }

    /**
     * Returns a formatted date string for the given date.
     */
    fun getCourseFormattedDate(context: Context, date: Date): String {
        val inputDate = Calendar.getInstance().also {
            it.time = date
            it.clearTimeComponents()
        }
        val daysDifference = getDayDifference(inputDate)

        return when {
            daysDifference == 0 -> {
                context.getString(R.string.core_date_format_today)
            }

            daysDifference == 1 -> {
                context.getString(R.string.core_date_format_tomorrow)
            }

            daysDifference == -1 -> {
                context.getString(R.string.core_date_format_yesterday)
            }

            daysDifference in -2 downTo -7 -> {
                context.getString(
                    R.string.core_date_format_days_ago,
                    ceil(-daysDifference.toDouble()).toInt().toString()
                )
            }

            daysDifference in 2..7 -> {
                DateUtils.formatDateTime(
                    context,
                    date.time,
                    DateUtils.FORMAT_SHOW_WEEKDAY
                )
            }

            inputDate.get(Calendar.YEAR) != Calendar.getInstance().get(Calendar.YEAR) -> {
                DateUtils.formatDateTime(
                    context,
                    date.time,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
                )
            }

            else -> {
                DateUtils.formatDateTime(
                    context,
                    date.time,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR
                )
            }
        }
    }

    fun getAssignmentFormattedDate(context: Context, date: Date): String {
        val inputDate = Calendar.getInstance().also {
            it.time = date
            it.clearTimeComponents()
        }
        val daysDifference = getDayDifference(inputDate)

        return when {
            daysDifference == 0 -> {
                context.getString(R.string.core_date_format_assignment_due_today)
            }

            daysDifference == 1 -> {
                context.getString(R.string.core_date_format_assignment_due_tomorrow)
            }

            daysDifference == -1 -> {
                context.getString(R.string.core_date_format_assignment_due_yesterday)
            }

            daysDifference <= -2 -> {
                val numberOfDays = ceil(-daysDifference.toDouble()).toInt()
                context.resources.getQuantityString(
                    R.plurals.core_date_format_assignment_due_days_ago,
                    numberOfDays,
                    numberOfDays
                )
            }

            else -> {
                val numberOfDays = ceil(daysDifference.toDouble()).toInt()
                context.resources.getQuantityString(
                    R.plurals.core_date_format_assignment_due_in,
                    numberOfDays,
                    numberOfDays
                )
            }
        }
    }

    fun getCourseAccessFormattedDate(context: Context, date: Date): String {
        val resourceManager = ResourceManager(context)
        return dateToCourseDate(resourceManager, date)
    }

    /**
     * Returns the number of days difference between the given date and the current date.
     */
    private fun getDayDifference(inputDate: Calendar): Int {
        val currentDate = Calendar.getInstance().also { it.clearTimeComponents() }
        val difference = inputDate.timeInMillis - currentDate.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(difference).toInt()
    }
}

/**
 * Extension function to clear time components of a calendar.
 * for example, if the time is 10:30:45, it will set the time to 00:00:00
 */
fun Calendar.clearTimeComponents() {
    this.set(Calendar.HOUR_OF_DAY, 0)
    this.set(Calendar.MINUTE, 0)
    this.set(Calendar.SECOND, 0)
    this.set(Calendar.MILLISECOND, 0)
}

/**
 * Extension function to check if the given date is today.
 */
fun Date.isToday(): Boolean {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.clearTimeComponents()
    return calendar.time == Date().clearTime()
}

/**
 * Extension function to add days to a date.
 * for example, if the date is 2020-01-01 10:30:45, and days is 2, it will return 2020-01-03 00:00:00
 */
fun Date.addDays(days: Int): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.clearTimeComponents()
    calendar.add(Calendar.DATE, days)
    return calendar.time
}

/**
 * Extension function to clear time components of a date.
 * for example, if the date is 2020-01-01 10:30:45, it will return 2020-01-01 00:00:00
 */
fun Date.clearTime(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.clearTimeComponents()
    return calendar.time
}

/**
 * Extension function to check if the time difference between the given date and the current date is less than 24 hours.
 */
fun Date.isTimeLessThan24Hours(): Boolean {
    val calendar = Calendar.getInstance()
    calendar.time = this
    val timeInMillis = (calendar.timeInMillis - TimeUtils.getCurrentTime()).unaryPlus()
    return timeInMillis < TimeUnit.DAYS.toMillis(1)
}

fun Date.toCalendar(): Calendar {
    val calendar = Calendar.getInstance()
    calendar.time = this
    return calendar
}
