package org.openedx.core.utils

import android.content.Context
import android.text.format.DateUtils
import com.google.gson.internal.bind.util.ISO8601Utils
import org.openedx.core.R
import org.openedx.core.domain.model.StartType
import org.openedx.foundation.system.ResourceManager
import java.text.DateFormat
import java.text.ParseException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

@Suppress("MagicNumber")
object TimeUtils {

    private const val FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    private const val FORMAT_ISO_8601_WITH_TIME_ZONE = "yyyy-MM-dd'T'HH:mm:ssXXX"
    private const val SEVEN_DAYS_IN_MILLIS = 604800000L

    fun formatToString(context: Context, date: Date, useRelativeDates: Boolean): String {
        if (!useRelativeDates) {
            val locale = Locale.Builder().setLanguage(Locale.getDefault().language).build()
            val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
            return dateFormat.format(date)
        }

        val now = Calendar.getInstance()
        val inputDate = Calendar.getInstance().apply { time = date }
        val daysDiff = ((now.timeInMillis - inputDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        return when {
            daysDiff in -5..-1 -> DateUtils.formatDateTime(
                context,
                date.time,
                DateUtils.FORMAT_SHOW_WEEKDAY
            ).toString()

            daysDiff == -6 -> context.getString(R.string.core_next) + " " + DateUtils.formatDateTime(
                context,
                date.time,
                DateUtils.FORMAT_SHOW_WEEKDAY
            ).toString()

            daysDiff in -1..1 -> DateUtils.getRelativeTimeSpanString(
                date.time,
                now.timeInMillis,
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_TIME
            ).toString()

            daysDiff in 2..6 -> DateUtils.getRelativeTimeSpanString(
                date.time,
                now.timeInMillis,
                DateUtils.DAY_IN_MILLIS
            ).toString()

            inputDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) -> {
                DateUtils.getRelativeTimeSpanString(
                    date.time,
                    now.timeInMillis,
                    DateUtils.DAY_IN_MILLIS,
                    DateUtils.FORMAT_SHOW_DATE
                ).toString()
            }

            else -> {
                DateUtils.getRelativeTimeSpanString(
                    date.time,
                    now.timeInMillis,
                    DateUtils.DAY_IN_MILLIS,
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR
                ).toString()
            }
        }
    }
    fun formatToDueInString(context: Context, date: Date): String {
        val now = Calendar.getInstance()
        val dueDate = Calendar.getInstance().apply { time = date }
        now.set(Calendar.HOUR_OF_DAY, 0)
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)
        dueDate.set(Calendar.HOUR_OF_DAY, 0)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)
        dueDate.set(Calendar.MILLISECOND, 0)
        val daysDifference =
            ((dueDate.timeInMillis - now.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
        return when {
            daysDifference < 0 -> context.getString(R.string.core_date_type_past_due)
            daysDifference == 0 -> context.getString(R.string.core_date_type_today)
            else -> context.getString(R.string.core_date_format_due_in_days, daysDifference)
        }
    }

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
            val courseDateFormat = SimpleDateFormat(
                FORMAT_ISO_8601,
                Locale.getDefault()
            )
            val applicationDateFormat = SimpleDateFormat(
                context.getString(R.string.core_full_date_with_time),
                Locale.getDefault()
            )
            applicationDateFormat.format(courseDateFormat.parse(text)!!)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    private fun dateToCourseDate(resourceManager: ResourceManager, date: Date?): String {
        return formatDate(
            format = resourceManager.getString(R.string.core_date_format_MMM_dd_yyyy),
            date = date
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
        val resourceManager = ResourceManager(context)

        return when {
            isDatePassed(today, start) -> handleDatePassedToday(
                resourceManager,
                today,
                expiry,
                start,
                end,
                startType,
                startDisplay
            )

            else -> handleDateNotPassedToday(resourceManager, start, startType, startDisplay)
        }
    }

    private fun handleDatePassedToday(
        resourceManager: ResourceManager,
        today: Date,
        expiry: Date?,
        start: Date?,
        end: Date?,
        startType: String,
        startDisplay: String
    ): String {
        return when {
            expiry != null -> handleExpiry(resourceManager, today, expiry)
            else -> handleNoExpiry(resourceManager, today, start, end, startType, startDisplay)
        }
    }

    private fun handleExpiry(resourceManager: ResourceManager, today: Date, expiry: Date): String {
        val dayDifferenceInMillis = (today.time - expiry.time).absoluteValue

        return when {
            isDatePassed(today, expiry) -> {
                if (dayDifferenceInMillis > SEVEN_DAYS_IN_MILLIS) {
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
                    resourceManager.getString(R.string.core_label_access_expired, timeSpan)
                }
            }

            else -> {
                if (dayDifferenceInMillis > SEVEN_DAYS_IN_MILLIS) {
                    resourceManager.getString(
                        R.string.core_label_expires,
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
        }
    }

    private fun handleNoExpiry(
        resourceManager: ResourceManager,
        today: Date,
        start: Date?,
        end: Date?,
        startType: String,
        startDisplay: String
    ): String {
        return when {
            end == null -> handleNoEndDate(resourceManager, start, startType, startDisplay)
            isDatePassed(today, end) -> resourceManager.getString(
                R.string.core_label_ended,
                dateToCourseDate(resourceManager, end)
            )

            else -> resourceManager.getString(
                R.string.core_label_ends,
                dateToCourseDate(resourceManager, end)
            )
        }
    }

    private fun handleDateNotPassedToday(
        resourceManager: ResourceManager,
        start: Date?,
        startType: String,
        startDisplay: String
    ): String {
        return when {
            startType == StartType.TIMESTAMP.type && start != null -> resourceManager.getString(
                R.string.core_label_starting,
                dateToCourseDate(resourceManager, start)
            )

            startType == StartType.STRING.type && start != null -> resourceManager.getString(
                R.string.core_label_starting,
                startDisplay
            )

            else -> {
                val soon = resourceManager.getString(R.string.core_assessment_soon)
                resourceManager.getString(R.string.core_label_starting, soon)
            }
        }
    }

    private fun handleNoEndDate(
        resourceManager: ResourceManager,
        start: Date?,
        startType: String,
        startDisplay: String
    ): String {
        return when {
            startType == StartType.TIMESTAMP.type && start != null -> resourceManager.getString(
                R.string.core_label_starting,
                dateToCourseDate(resourceManager, start)
            )

            startType == StartType.STRING.type && start != null -> resourceManager.getString(
                R.string.core_label_starting,
                startDisplay
            )

            else -> {
                val soon = resourceManager.getString(R.string.core_assessment_soon)
                resourceManager.getString(R.string.core_label_starting, soon)
            }
        }
    }

    /**
     * Returns a formatted date string for the given date using context.
     */
    fun getCourseAccessFormattedDate(context: Context, date: Date): String {
        val resourceManager = ResourceManager(context)
        return dateToCourseDate(resourceManager, date)
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

fun Date.toCalendar(): Calendar {
    val calendar = Calendar.getInstance()
    calendar.time = this
    return calendar
}
