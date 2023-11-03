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

object TimeUtils {

    private const val FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    private const val FORMAT_ISO_8601_WITH_TIME_ZONE = "yyyy-MM-dd'T'HH:mm:ssXXX"
    private const val FORMAT_APPLICATION = "dd.MM.yyyy HH:mm"
    const val FORMAT_DATE = "dd MMM, yyyy"
    const val FORMAT_DATE_TAB = "EEE, MMM dd, yyyy"

    private const val SEVEN_DAYS_IN_MILLIS = 604800000L

    fun getCurrentTimeInSeconds(): Long {
        return TimeUnit.MILLISECONDS.toSeconds(Calendar.getInstance().timeInMillis)
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

    /**
     * This method used to convert the date to ISO 8601 compliant format date string
     * @param date [Date]needs to be converted
     * @return The current date and time in a ISO 8601 compliant format.
     */
    fun dateToIso8601(date: Date?): String {
        return ISO8601Utils.format(date, true)
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

    fun dateToCourseDate(resourceManager: ResourceManager, date: Date?): String {
        return formatDate(
            format = resourceManager.getString(R.string.core_date_format_MMMM_dd), date = date
        )
    }

    fun formatDate(format: String, date: String): String {
        return formatDate(format, iso8601ToDate(date))
    }

    fun formatDate(format: String, date: Date?): String {
        if (date == null) {
            return ""
        }
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        return sdf.format(date)
    }

    fun stringToDate(dateFormat: String, date: String): Date? {
        if (dateFormat.isEmpty() || date.isEmpty()) {
            return null
        }
        return SimpleDateFormat(dateFormat, Locale.getDefault()).parse(date)
    }

    /**
     * Checks if the given date is past today.
     *
     * @param today     Today's date.
     * @param otherDate Other date to cross-match with today's date.
     * @return <code>true</code> if the other date is past today,
     * <code>false</code> otherwise.
     */
    fun isDatePassed(today: Date, otherDate: Date?): Boolean {
        return otherDate != null && today.after(otherDate)
    }

    /**
     * This function compare the provide date with current date
     * @param today     Today's date.
     * @param otherDate Other date to cross-match with today's date.
     * @return <code>true</code> if the other date is due today,
     */
    fun isDueDate(today: Date, otherDate: Date?): Boolean {
        return otherDate != null && today.before(otherDate)
    }

    /**
     * This function compare the provide date are same
     * @return true if the provided date are same else false
     */
    fun areDatesSame(date: Date?, otherDate: Date?): Boolean {
        return date != null && otherDate != null &&
                formatDate(FORMAT_DATE, date) == formatDate(FORMAT_DATE, otherDate)
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
                        R.string.core_label_ending, dateToCourseDate(resourceManager, end)
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
}
