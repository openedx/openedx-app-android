package org.openedx.profile.system

import android.content.Context
import android.content.Intent
import org.openedx.profile.service.CalendarSyncService

class CalendarSyncServiceInitiator(private val context: Context) {

    fun startSyncCalendarService() {
        val intent = Intent(context, CalendarSyncService::class.java)
        context.startService(intent)
    }
}
