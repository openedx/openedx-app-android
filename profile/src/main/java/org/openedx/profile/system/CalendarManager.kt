package org.openedx.profile.system

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import org.openedx.core.R
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CalendarData
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.system.ResourceManager
import org.openedx.core.utils.Logger
import org.openedx.core.utils.toCalendar
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import org.openedx.core.R as CoreR

class CalendarManager(
    private val context: Context,
    private val corePreferences: CorePreferences,
    private val resourceManager: ResourceManager,
) {
    private val logger = Logger(TAG)

    val permissions = arrayOf(
        android.Manifest.permission.WRITE_CALENDAR,
        android.Manifest.permission.READ_CALENDAR
    )

    private val accountName: String
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
    fun isCalendarExists(calendarTitle: String): Boolean {
        if (hasPermissions()) {
            return getCalendarId(calendarTitle) != CALENDAR_DOES_NOT_EXIST
        }
        return false
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
     * Method to check if the calendar with the course name exist in the mobile calendar app or not
     */
    @SuppressLint("Range")
    fun getCalendarId(calendarTitle: String): Long {
        var calendarId = CALENDAR_DOES_NOT_EXIST
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.NAME
        )
        val calendarContentResolver = context.contentResolver
        val cursor = calendarContentResolver.query(
            CalendarContract.Calendars.CONTENT_URI, projection,
            CalendarContract.Calendars.ACCOUNT_NAME + "=? and (" +
                    CalendarContract.Calendars.NAME + "=? or " +
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + "=?)", arrayOf(
                accountName, calendarTitle,
                calendarTitle
            ), null
        )
        if (cursor?.moveToFirst() == true) {
            if (cursor.getString(cursor.getColumnIndex(CalendarContract.Calendars.NAME))
                    .equals(calendarTitle)
            ) {
                calendarId =
                    cursor.getInt(cursor.getColumnIndex(CalendarContract.Calendars._ID)).toLong()
            }
        }
        cursor?.close()
        return calendarId
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
                "${resourceManager.getString(R.string.core_assignment_due_tag)} : $courseName"
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
        val eventDescription = courseDateBlock.title
        // The following code for branch and deep links will be enabled after implementation
        /*
        if (isDeeplinkEnabled && !TextUtils.isEmpty(courseDateBlock.blockId)) {
            val metaData = ContentMetadata()
                .addCustomMetadata(DeepLink.Keys.SCREEN_NAME, Screen.COURSE_COMPONENT)
                .addCustomMetadata(DeepLink.Keys.COURSE_ID, courseId)
                .addCustomMetadata(DeepLink.Keys.COMPONENT_ID, courseDateBlock.blockId)

            val branchUniversalObject = BranchUniversalObject()
                .setCanonicalIdentifier("${Screen.COURSE_COMPONENT}\n${courseDateBlock.blockId}")
                .setTitle(courseDateBlock.title)
                .setContentDescription(courseDateBlock.title)
                .setContentMetadata(metaData)

            val linkProperties = LinkProperties()
                .addControlParameter("\$desktop_url", courseDateBlock.link)

            eventDescription += "\n" + branchUniversalObject.getShortUrl(context, linkProperties)
        }
         */
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
     * Method to query the events for the given calendar id
     *
     * @param calendarId calendarId to query the events
     *
     * @return [Cursor]
     *
     * */
    private fun getCalendarEvents(calendarId: Long): Cursor? {
        val calendarContentResolver = context.contentResolver
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DESCRIPTION
        )
        val selection = CalendarContract.Events.CALENDAR_ID + "=?"
        return calendarContentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            arrayOf(calendarId.toString()),
            null
        )
    }

    /**
     * Method to compare the calendar events with course dates
     * @return  true if the events are the same as calendar dates otherwise false
     */
    @SuppressLint("Range")
    private fun compareEvents(
        calendarId: Long,
        courseDateBlocks: List<CourseDateBlock>
    ): Boolean {
        val cursor = getCalendarEvents(calendarId) ?: return false

        val datesList = ArrayList(courseDateBlocks)
        val dueDateColumnIndex = cursor.getColumnIndex(CalendarContract.Events.DTEND)
        val descriptionColumnIndex = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)

        while (cursor.moveToNext()) {
            val dueDateInMillis = cursor.getLong(dueDateColumnIndex)

            val description = cursor.getString(descriptionColumnIndex)
            if (description != null) {
                val matchedDate = datesList.find { unit ->
                    description.contains(unit.title, ignoreCase = true)
                }

                matchedDate?.let { unit ->
                    val dueDateCalendar = Calendar.getInstance().apply {
                        timeInMillis = dueDateInMillis
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val unitDateCalendar = unit.date.toCalendar().apply {
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (dueDateCalendar == unitDateCalendar) {
                        datesList.remove(unit)
                    } else {
                        // If any single value isn't matched, return false
                        cursor.close()
                        return false
                    }
                }
            }
        }

        cursor.close()
        return datesList.isEmpty()
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

    fun openCalendarApp() {
        val builder: Uri.Builder = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time")
        ContentUris.appendId(builder, Calendar.getInstance().timeInMillis)
        val intent = Intent(Intent.ACTION_VIEW).setData(builder.build())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Helper method used to check that the calendar if outdated for the course or not
     *
     * @param calendarTitle Title for the course Calendar
     * @param courseDateBlocks Course dates events
     *
     * @return Calendar Id if Calendar is outdated otherwise -1 or CALENDAR_DOES_NOT_EXIST
     *
     */
    fun isCalendarOutOfDate(
        calendarTitle: String,
        courseDateBlocks: List<CourseDateBlock>
    ): Long {
        if (isCalendarExists(calendarTitle)) {
            val calendarId = getCalendarId(calendarTitle)
            if (compareEvents(calendarId, courseDateBlocks).not()) {
                return calendarId
            }
        }
        return CALENDAR_DOES_NOT_EXIST
    }

    /**
     * Method to get the current user account as the Calendar owner
     *
     * @return calendar owner account or "local_user"
     */
    private fun getUserAccountForSync(): String {
        return corePreferences.user?.email ?: LOCAL_USER
    }

    /**
     * Method to create the Calendar title for the platform against the course
     *
     * @param courseName Name of the course for that creating the Calendar events.
     *
     * @return title of the Calendar against the course
     */
    fun getCourseCalendarTitle(courseName: String): String {
        return "${resourceManager.getString(id = CoreR.string.platform_name)} - $courseName"
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

    companion object {
        const val CALENDAR_DOES_NOT_EXIST = -1L
        const val EVENT_DOES_NOT_EXIST = -1L
        private const val TAG = "CalendarManager"
        private const val LOCAL_USER = "local_user"
    }
}
