package org.openedx.core.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.openedx.core.system.CalendarManager

class CalendarSyncService : Service() {

    private val calendarManager: CalendarManager by inject()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        CoroutineScope(Dispatchers.IO).launch {
            syncCalendar()
        }
        return START_STICKY
    }

    private fun syncCalendar() {
        Log.e("___", "syncCalendar")
    }
}
