package org.openedx.core.system

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.ContentMetadata
import io.branch.referral.util.LinkProperties
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CalendarData
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.utils.Logger
import org.openedx.core.utils.toCalendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class CalendarManager(
    private val context: Context,
    private val corePreferences: CorePreferences,
) {
    private val logger = Logger(TAG)

    val permissions = arrayOf(
        android.Manifest.permission.WRITE_CALENDAR,
        android.Manifest.permission.READ_CALENDAR
    )

    val accountName: String
        get() = getUserAccountForSync()

    /**
     * Check if the app has the calendar READ/WRITE permissions or not
     */
    fun hasPermissions(): Boolean = permissions.all {
        PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, it)
    }

    /**
     * Check if the calendar is already existed in mobile calendar app or not
     */
    fun isCalendarExist(calendarId: Long): Boolean {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars._ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())

        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        val exists = cursor != null && cursor.count > 0
        cursor?.close()

        return exists
    }

    /**
     * Create or update the calendar if it is already existed in mobile calendar app
     */
    fun createOrUpdateCalendar(
        calendarId: Long = CALENDAR_DOES_NOT_EXIST,
        calendarTitle: String,
        calendarColor: Long
    ): Long {
        if (calendarId != CALENDAR_DOES_NOT_EXIST) {
            deleteCalendar(calendarId = calendarId)
        }

        return createCalendar(
            calendarTitle = calendarTitle,
            calendarColor = calendarColor
        )
    }

    /**
     * Method to create a separate calendar based on course name in mobile calendar app
     */
    private fun createCalendar(
        calendarTitle: String,
        calendarColor: Long
    ): Long {
        val contentValues = ContentValues()
        contentValues.put(CalendarContract.Calendars.NAME, calendarTitle)
        contentValues.put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, calendarTitle)
        contentValues.put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
        contentValues.put(
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.ACCOUNT_TYPE_LOCAL
        )
        contentValues.put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
        contentValues.put(
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CAL_ACCESS_ROOT
        )
        contentValues.put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        contentValues.put(CalendarContract.Calendars.VISIBLE, 1)
        contentValues.put(
            CalendarContract.Calendars.CALENDAR_COLOR,
            calendarColor.toInt()
        )
        val creationUri: Uri? = asSyncAdapter(
            Uri.parse(CalendarContract.Calendars.CONTENT_URI.toString()),
            accountName
        )
        creationUri?.let {
            val calendarData: Uri? = context.contentResolver.insert(creationUri, contentValues)
            calendarData?.let {
                val id = calendarData.lastPathSegment?.toLong()
                logger.d { "Calendar ID $id" }
                return id ?: CALENDAR_DOES_NOT_EXIST
            }
        }
        return CALENDAR_DOES_NOT_EXIST
    }

    /**
     * Method to add important dates of course as calendar event into calendar of mobile app
     */
    fun addEventsIntoCalendar(
        calendarId: Long,
        courseId: String,
        courseName: String,
        courseDateBlock: CourseDateBlock
    ): Long {
        val date = courseDateBlock.date.toCalendar()
        // start time of the event, adjusted 1 hour earlier for a 1-hour duration
        val startMillis: Long = date.timeInMillis - TimeUnit.HOURS.toMillis(1)
        // end time of the event added to the calendar
        val endMillis: Long = date.timeInMillis

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(
                CalendarContract.Events.TITLE,
                "${courseDateBlock.title} : $courseName"
            )
            put(
                CalendarContract.Events.DESCRIPTION,
                getEventDescription(
                    courseId = courseId,
                    courseDateBlock = courseDateBlock,
                    isDeeplinkEnabled = corePreferences.appConfig.courseDatesCalendarSync.isDeepLinkEnabled
                )
            )
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        uri?.let { addReminderToEvent(uri = it) }
        val eventId = uri?.lastPathSegment?.toLong() ?: EVENT_DOES_NOT_EXIST
        return eventId
    }

    /**
     * Method to generate & add deeplink into event description
     *
     * @return event description with deeplink for assignment block else block title
     */
    private fun getEventDescription(
        courseId: String,
        courseDateBlock: CourseDateBlock,
        isDeeplinkEnabled: Boolean
    ): String {
        var eventDescription = courseDateBlock.description

        if (isDeeplinkEnabled && courseDateBlock.blockId.isNotEmpty()) {
            val metaData = ContentMetadata()
                .addCustomMetadata("screen_name", "course_component")
                .addCustomMetadata("course_id", courseId)
                .addCustomMetadata("component_id", courseDateBlock.blockId)

            val branchUniversalObject = BranchUniversalObject()
                .setCanonicalIdentifier("course_component\n${courseDateBlock.blockId}")
                .setTitle(courseDateBlock.title)
                .setContentDescription(courseDateBlock.title)
                .setContentMetadata(metaData)

            val linkProperties = LinkProperties()
                .addControlParameter("\$desktop_url", courseDateBlock.link)

            val shortUrl = branchUniversalObject.getShortUrl(context, linkProperties)
            eventDescription += "\n$shortUrl"
        }

        return eventDescription
    }

    /**
     * Method to add a reminder to the given calendar events
     *
     * @param uri Calendar event Uri
     */
    private fun addReminderToEvent(uri: Uri) {
        val eventId: Long? = uri.lastPathSegment?.toLong()
        logger.d { "Event ID $eventId" }

        // Adding reminder on the start of event
        val eventValues = ContentValues().apply {
            put(CalendarContract.Reminders.MINUTES, 0)
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, eventValues)
        // Adding reminder 24 hours before the event get started
        eventValues.apply {
            put(CalendarContract.Reminders.MINUTES, TimeUnit.DAYS.toMinutes(1))
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, eventValues)
        // Adding reminder 48 hours before the event get started
        eventValues.apply {
            put(CalendarContract.Reminders.MINUTES, TimeUnit.DAYS.toMinutes(2))
        }
        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, eventValues)
    }

    /**
     * Method to delete the course calendar from the mobile calendar app
     */
    fun deleteCalendar(calendarId: Long) {
        context.contentResolver.delete(
            Uri.parse("content://com.android.calendar/calendars/$calendarId"),
            null,
            null
        )
    }

    /**
     * Helper method used to return a URI for use with a sync adapter (how an application and a
     * sync adapter access the Calendar Provider)
     *
     * @param uri URI to access the calendar
     * @param account Name of the calendar owner
     *
     * @return URI of the calendar
     *
     */
    private fun asSyncAdapter(uri: Uri, account: String): Uri? {
        return uri.buildUpon().appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.SyncState.ACCOUNT_NAME, account)
            .appendQueryParameter(
                CalendarContract.SyncState.ACCOUNT_TYPE,
                CalendarContract.ACCOUNT_TYPE_LOCAL
            ).build()
    }

    /**
     * Method to get the current user account as the Calendar owner
     *
     * @return calendar owner account or "local_user"
     */
    private fun getUserAccountForSync(): String {
        return corePreferences.user?.email ?: LOCAL_USER
    }

    fun getCalendarData(calendarId: Long): CalendarData? {
        val projection = arrayOf(
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )
        val selection = "${CalendarContract.Calendars._ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())

        val cursor: Cursor? = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        return cursor?.use {
            if (it.moveToFirst()) {
                val title = it.getString(it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                val color = it.getInt(it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR))
                CalendarData(
                    title = title,
                    color = color
                )
            } else {
                null
            }
        }
    }

    fun deleteEvent(eventId: Long) {
        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val rows = context.contentResolver.delete(deleteUri, null, null)
        if (rows > 0) {
            logger.d { "Event deleted successfully" }
        } else {
            logger.d { "Event deletion failed" }
        }
    }

    companion object {
        const val CALENDAR_DOES_NOT_EXIST = -1L
        const val EVENT_DOES_NOT_EXIST = -1L
        private const val TAG = "CalendarManager"
        private const val LOCAL_USER = "local_user"
    }
}
