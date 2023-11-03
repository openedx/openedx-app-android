package org.openedx.core.utils

import android.content.Context
import android.text.format.DateUtils
import org.openedx.core.R
import org.openedx.core.domain.model.StartType
import org.openedx.core.system.ResourceManager
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    private const val FORMAT_ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    private const val FORMAT_ISO_8601_WITH_TIME_ZONE = "yyyy-MM-dd'T'HH:mm:ssXXX"
    private const val FORMAT_APPLICATION = "dd.MM.yyyy HH:mm"
    private const val FORMAT_DATE = "dd MMM, yyyy"

    private const val SEVEN_DAYS_IN_MILLIS = 604800000L

    fun iso8601ToDate(text: String): Date? {
        return try {
            val sdf = SimpleDateFormat(FORMAT_ISO_8601, Locale.getDefault())
            sdf.parse(text)
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

    fun iso8601ToDateWithTime(context: Context,text: String): String {
        return try {
            val courseDateFormat = SimpleDateFormat(FORMAT_ISO_8601, Locale.getDefault())
            val applicationDateFormat = SimpleDateFormat(context.getString(R.string.core_full_date_with_time), Locale.getDefault())
            applicationDateFormat.format(courseDateFormat.parse(text)!!)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun dateToCourseDate(resourceManager: ResourceManager, date: Date?): String {
        if (date == null) {
            return ""
        }
        val sdf = SimpleDateFormat(
            resourceManager.getString(R.string.core_date_format_MMMM_dd),
            Locale.getDefault()
        )
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
    fun isDatePassed(today: Date, otherDate: Date?): Boolean {
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
        var formattedDate = ""
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
                            expiry.time, today.time,
                            DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
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
                            expiry.time, today.time,
                            DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
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
                        R.string.core_label_ended,
                        dateToCourseDate(resourceManager, end)
                    )
                } else {
                    resourceManager.getString(
                        R.string.core_label_ending,
                        dateToCourseDate(resourceManager, end)
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