package org.openedx.core.system

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.util.ContentMetadata
import io.branch.referral.util.LinkProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.domain.model.CalendarData
import org.openedx.core.domain.model.CalendarType
import org.openedx.core.domain.model.CourseDateBlock
import org.openedx.core.domain.model.UserCalendar
import org.openedx.core.utils.Logger
import org.openedx.core.utils.toCalendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class CalendarManager(
    private val context: Context,
    private val corePreferences: CorePreferences,
) {
    private val logger = Logger(TAG)

    private data class CalendarAccount(val name: String, val type: String)

    val permissions = arrayOf(
        android.Manifest.permission.WRITE_CALENDAR,
        android.Manifest.permission.READ_CALENDAR
    )

    val accountName: String
        get() = getUserAccountForSync()

    fun hasPermissions(): Boolean = permissions.all {
        PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, it)
    }

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

    fun createOrUpdateCalendar(
        calendarId: Long = CALENDAR_DOES_NOT_EXIST,
        calendarTitle: String,
        calendarColor: Long,
        calendarType: CalendarType
    ): Long {
        if (calendarId != CALENDAR_DOES_NOT_EXIST && calendarType == CalendarType.LOCAL) {
            deleteCalendar(calendarId = calendarId)
        }

        return createCalendar(
            calendarTitle = calendarTitle,
            calendarColor = calendarColor,
            calendarType = calendarType
        )
    }

    private fun createCalendar(
        calendarTitle: String,
        calendarColor: Long,
        calendarType: CalendarType
    ): Long {
        if (calendarType == CalendarType.GOOGLE) {
            val existingGoogleCalendar = findOrCreateGoogleCalendar()
            if (existingGoogleCalendar != CALENDAR_DOES_NOT_EXIST) {
                return existingGoogleCalendar
            }
        }

        val calendarAccount = when (calendarType) {
            CalendarType.LOCAL -> CalendarAccount(accountName, CalendarContract.ACCOUNT_TYPE_LOCAL)
            CalendarType.GOOGLE -> getCalendarOwnerAccount()
        }
        val contentValues = ContentValues().apply {
            put(CalendarContract.Calendars.NAME, calendarTitle)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, calendarTitle)
            put(CalendarContract.Calendars.ACCOUNT_NAME, calendarAccount.name)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, calendarAccount.type)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, calendarAccount.name)
            put(
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.CAL_ACCESS_ROOT
            )
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(
                CalendarContract.Calendars.CALENDAR_COLOR,
                calendarColor.toInt()
            )
        }

        val calendarData = context.contentResolver.insert(
            CalendarContract.Calendars.CONTENT_URI,
            contentValues
        )

        return calendarData?.lastPathSegment?.toLong()?.also {
            logger.d { "Calendar ID $it created" }
        } ?: CALENDAR_DOES_NOT_EXIST
    }

    private fun findOrCreateGoogleCalendar(): Long {
        return findPrimaryGoogleCalendar()?.also {
            logger.d { "Using existing primary Google Calendar ID $it" }
        } ?: findWritableGoogleCalendar()?.also {
            logger.d { "Using existing Google Calendar ID $it" }
        } ?: run {
            logger.d { "No Google Calendar found, will create local calendar" }
            CALENDAR_DOES_NOT_EXIST
        }
    }

    private fun findPrimaryGoogleCalendar(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.ACCOUNT_TYPE} = ? AND " +
                "${CalendarContract.Calendars.IS_PRIMARY} = 1 AND " +
                "${CalendarContract.Calendars.SYNC_EVENTS} = 1 AND " +
                "${CalendarContract.Calendars.VISIBLE} = 1"
        val selectionArgs = arrayOf(GOOGLE_ACCOUNT_TYPE)

        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        return cursor?.use {
            if (it.moveToFirst()) {
                it.getLong(it.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
            } else {
                null
            }
        }
    }

    private fun findWritableGoogleCalendar(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        val selection = "${CalendarContract.Calendars.ACCOUNT_TYPE} = ? AND " +
                "${CalendarContract.Calendars.SYNC_EVENTS} = 1 AND " +
                "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf(
            GOOGLE_ACCOUNT_TYPE,
            CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()
        )

        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Calendars.IS_PRIMARY} DESC"
        )

        return cursor?.use {
            if (it.moveToFirst()) {
                it.getLong(it.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
            } else {
                null
            }
        }
    }

    fun getGoogleCalendars(): List<UserCalendar> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR
        )
        val selection = "${CalendarContract.Calendars.ACCOUNT_TYPE} = ? AND " +
                "${CalendarContract.Calendars.SYNC_EVENTS} = 1 AND " +
                "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
                "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?"
        val selectionArgs = arrayOf(
            GOOGLE_ACCOUNT_TYPE,
            CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()
        )
        val sortOrder =
            "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC"

        return try {
            val cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val idIndex = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val titleIndex =
                    it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val colorIndex = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)

                buildList {
                    while (it.moveToNext()) {
                        add(
                            UserCalendar(
                                id = it.getLong(idIndex),
                                title = it.getString(titleIndex),
                                color = it.getInt(colorIndex)
                            )
                        )
                    }
                }
            } ?: emptyList()
        } catch (e: SecurityException) {
            logger.d { "Failed to load Google calendars: ${e.message}" }
            emptyList()
        }
    }

    fun hasAlternativeCalendarApp(): Boolean {
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        val activities = context.packageManager.queryIntentActivities(intent, 0)
        return activities.any { it.activityInfo.packageName != GOOGLE_CALENDAR_PACKAGE }
    }

    fun addEventsIntoCalendar(
        calendarId: Long,
        courseId: String,
        courseName: String,
        courseDateBlock: CourseDateBlock
    ): Long {
        repeat(EVENT_ATTEMPTS) { attemptIndex ->
            val attemptNumber = attemptIndex + 1
            val eventId =
                tryCreateEvent(calendarId, courseId, courseName, courseDateBlock, attemptNumber)
            if (eventId != EVENT_DOES_NOT_EXIST) {
                return eventId
            }
            if (attemptNumber < EVENT_ATTEMPTS) {
                runBlocking { delay(ACTION_RETRY_DELAY) }
            }
        }
        logger.d { "Failed to create event after $EVENT_ATTEMPTS attempts" }
        return EVENT_DOES_NOT_EXIST
    }

    private fun tryCreateEvent(
        calendarId: Long,
        courseId: String,
        courseName: String,
        courseDateBlock: CourseDateBlock,
        attemptNumber: Int
    ): Long {
        return try {
            val date = courseDateBlock.date.toCalendar()
            val startMillis = date.timeInMillis - TimeUnit.HOURS.toMillis(1)
            val endMillis = date.timeInMillis

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
            val insertedEventId = uri?.lastPathSegment?.toLong() ?: EVENT_DOES_NOT_EXIST

            if (insertedEventId != EVENT_DOES_NOT_EXIST && isEventExists(insertedEventId)) {
                uri?.let { addReminderToEvent(uri = it) }
                logger.d { "Event created successfully: $insertedEventId (attempt $attemptNumber)" }
                insertedEventId
            } else {
                logger.d { "Event creation failed, retrying... (attempt $attemptNumber/$EVENT_ATTEMPTS)" }
                EVENT_DOES_NOT_EXIST
            }
        } catch (e: Exception) {
            logger.d { "Event creation error on attempt $attemptNumber: ${e.message}" }
            EVENT_DOES_NOT_EXIST
        }
    }

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

    private fun addReminderToEvent(uri: Uri) {
        val eventId = uri.lastPathSegment?.toLong() ?: return
        logger.d { "Event ID $eventId" }

        val eventValues = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }

        listOf(0, TimeUnit.DAYS.toMinutes(1), TimeUnit.DAYS.toMinutes(2)).forEach { minutes ->
            eventValues.put(CalendarContract.Reminders.MINUTES, minutes)
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, eventValues)
        }
    }

    fun deleteCalendar(calendarId: Long) {
        val calendarAccount = getCalendarAccountById(calendarId)
        if (calendarAccount?.type == GOOGLE_ACCOUNT_TYPE) {
            logger.d { "Cannot delete Google Calendar" }
            return
        }

        val calendarUri = ContentUris.withAppendedId(
            CalendarContract.Calendars.CONTENT_URI,
            calendarId
        )
        val rowsDeleted = context.contentResolver.delete(calendarUri, null, null)
        logger.d {
            if (rowsDeleted > 0) {
                "Calendar $calendarId deleted successfully"
            } else {
                "Calendar $calendarId deletion failed or calendar doesn't exist"
            }
        }
    }

    suspend fun deleteEvents(eventIds: List<Long>) {
        val deletedCount = eventIds.count { eventId ->
            var deleted = false
            var attempts = 0

            while (!deleted && attempts < EVENT_ATTEMPTS) {
                attempts++
                try {
                    deleted = deleteEventWithRetry(eventId)
                    if (!deleted && attempts < EVENT_ATTEMPTS) {
                        delay(ACTION_RETRY_DELAY)
                    }
                } catch (e: Exception) {
                    logger.d { "Failed to delete event $eventId on attempt $attempts: ${e.message}" }
                    if (attempts < EVENT_ATTEMPTS) {
                        delay(ACTION_RETRY_DELAY)
                    }
                }
            }

            if (!deleted) {
                logger.d { "Failed to delete event $eventId after $EVENT_ATTEMPTS attempts" }
            }

            deleted
        }
        logger.d { "Successfully deleted $deletedCount out of ${eventIds.size} events" }
    }

    private fun deleteEventWithRetry(eventId: Long): Boolean {
        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val rows = context.contentResolver.delete(deleteUri, null, null)

        val deleted = if (rows > 0) {
            val stillExists = isEventExists(eventId)
            if (!stillExists) {
                logger.d { "Event $eventId deleted successfully" }
                true
            } else {
                logger.d { "Event $eventId deletion reported success but event still exists" }
                false
            }
        } else {
            val exists = isEventExists(eventId)
            if (!exists) {
                logger.d { "Event $eventId doesn't exist (already deleted)" }
                true
            } else {
                logger.d { "Event $eventId deletion failed" }
                false
            }
        }
        return deleted
    }

    private fun getCalendarOwnerAccount(): CalendarAccount {
        return getSyncedAccountByType(GOOGLE_ACCOUNT_TYPE)
            ?: getFirstSyncedAccount(excludeLocal = true)
            ?: CalendarAccount(accountName, CalendarContract.ACCOUNT_TYPE_LOCAL)
    }

    private fun getFirstSyncedAccount(excludeLocal: Boolean): CalendarAccount? {
        val selection = buildString {
            append("${CalendarContract.Calendars.SYNC_EVENTS} = 1 AND ${CalendarContract.Calendars.VISIBLE} = 1")
            if (excludeLocal) {
                append(" AND ${CalendarContract.Calendars.ACCOUNT_TYPE} != ?")
            }
        }
        val selectionArgs = if (excludeLocal) {
            arrayOf(CalendarContract.ACCOUNT_TYPE_LOCAL)
        } else {
            null
        }

        return queryCalendarAccount(selection, selectionArgs)
    }

    private fun getSyncedAccountByType(accountType: String): CalendarAccount? {
        val selection =
            "${CalendarContract.Calendars.ACCOUNT_TYPE} = ? AND " +
                    "${CalendarContract.Calendars.SYNC_EVENTS} = 1 AND " +
                    "${CalendarContract.Calendars.VISIBLE} = 1"
        val selectionArgs = arrayOf(accountType)

        return queryCalendarAccount(selection, selectionArgs)
    }

    private fun queryCalendarAccount(
        selection: String,
        selectionArgs: Array<String>?
    ): CalendarAccount? {
        val projection = arrayOf(
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY
        )
        val sortOrder = "${CalendarContract.Calendars.IS_PRIMARY} DESC"

        val cursor = context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )

        return cursor?.use {
            if (it.moveToFirst()) {
                val accountName = it.getString(
                    it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                )
                val accountType = it.getString(
                    it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
                )
                CalendarAccount(accountName, accountType)
            } else {
                null
            }
        }
    }

    private fun getCalendarAccountById(calendarId: Long): CalendarAccount? {
        val selection = "${CalendarContract.Calendars._ID} = ?"
        val selectionArgs = arrayOf(calendarId.toString())
        return queryCalendarAccount(selection, selectionArgs)
    }

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
                val title =
                    it.getString(it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                val color =
                    it.getInt(it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR))
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
        logger.d {
            if (rows > 0) {
                "Event deleted successfully"
            } else {
                "Event deletion failed"
            }
        }
    }

    private fun isEventExists(eventId: Long): Boolean {
        if (eventId == EVENT_DOES_NOT_EXIST) return false

        val projection = arrayOf(CalendarContract.Events._ID)
        val selection = "${CalendarContract.Events._ID} = ?"
        val selectionArgs = arrayOf(eventId.toString())

        val cursor = context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )

        return cursor?.use {
            it.count > 0
        } ?: false
    }

    companion object {
        const val CALENDAR_DOES_NOT_EXIST = -1L
        const val EVENT_DOES_NOT_EXIST = -1L
        private const val TAG = "CalendarManager"
        private const val LOCAL_USER = "local_user"
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
        private const val GOOGLE_CALENDAR_PACKAGE = "com.google.android.calendar"

        private const val ACTION_RETRY_DELAY = 500L
        private const val EVENT_ATTEMPTS = 3
    }
}
