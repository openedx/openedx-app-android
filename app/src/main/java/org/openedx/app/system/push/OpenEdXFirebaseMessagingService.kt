package org.openedx.app.system.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.braze.push.BrazeFirebaseMessagingService
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.koin.android.ext.android.inject
import org.openedx.app.AppActivity
import org.openedx.app.R
import org.openedx.core.config.Config
import org.openedx.core.data.storage.CorePreferences

class OpenEdXFirebaseMessagingService : FirebaseMessagingService() {

    private val preferences: CorePreferences by inject()
    private val config: Config by inject()

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        if (BrazeFirebaseMessagingService.handleBrazeRemoteMessage(this, message)) {
            // This Remote Message originated from Braze and a push notification was displayed.
            // No further action is needed.
            return
        } else {
            // This Remote Message did not originate from Braze.
            handlePushNotification(message)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        preferences.pushToken = token
        if (preferences.user != null) {
            SyncFirebaseTokenWorker.schedule(this)
        }
    }

    private fun handlePushNotification(message: RemoteMessage) {
        val notification = message.notification ?: return
        val data = message.data

        val intent = Intent(this, AppActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        data.forEach { (k, v) ->
            intent.putExtra(k, v)
        }

        val code = createId()
        val pendingIntent = PendingIntent.getActivity(
            this,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = "${config.getPlatformName()}_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notification.title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notification.body)
            )
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                config.getPlatformName(),
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(code, notificationBuilder.build())
    }

    private fun createId(): Int {
        return SystemClock.uptimeMillis().toInt()
    }
}
