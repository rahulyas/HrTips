package com.rahul.hrtips

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class FirebaseMessageService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "FirebaseMsgService"
    }
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remotemsg: RemoteMessage) {
        Log.d(TAG, "From -> " + remotemsg.from)

        remotemsg.notification?.let {
            Log.d(TAG, "Demo Notification Body -> " + it.body)
            it.body?.let { sendNotification(it) }
        }

        remotemsg.data.isNotEmpty().let {
            // Handle data payload if needed
            if (it) {
                val data = remotemsg.data
                // Process the data payload
            }
        }
    }

    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(this)
            .setSmallIcon(getNotificationIcon())
            .setContentTitle("HrTips")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(0, notificationBuilder.build())
        }
    }

    fun getNotificationIcon():Int{
        val useWhiteIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        return if (useWhiteIcon) R.mipmap.ic_launcher else R.mipmap.ic_launcher_foreground
    }
}
