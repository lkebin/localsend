package org.localsend.localsend_app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val CHANNEL_ID = "localsend_receive_request"
private const val NOTIFICATION_ID = 2001

const val ACTION_ACCEPT = "org.localsend.localsend_app.ACTION_ACCEPT"
const val ACTION_DECLINE = "org.localsend.localsend_app.ACTION_DECLINE"
const val EXTRA_SESSION_ID = "session_id"

/**
 * Receives Accept / Decline taps from the incoming-file-request notification
 * and forwards the decision to Flutter via a MethodChannel call on [MainActivity].
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        val accepted = intent.action == ACTION_ACCEPT

        // Cancel the notification
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)

        // Bring the app to the foreground so Flutter can handle the result
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("action", if (accepted) "accept" else "decline")
            putExtra(EXTRA_SESSION_ID, sessionId)
        }
        context.startActivity(launchIntent)
    }
}

/**
 * Shows a notification asking the user to accept or decline an incoming file request.
 */
fun showReceiveRequestNotification(
    context: Context,
    sessionId: String,
    senderAlias: String,
    fileCount: Int,
) {
    createReceiveRequestChannel(context)

    val acceptIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_ACCEPT
            putExtra(EXTRA_SESSION_ID, sessionId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val declineIntent = PendingIntent.getBroadcast(
        context,
        1,
        Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_DECLINE
            putExtra(EXTRA_SESSION_ID, sessionId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val contentText = if (fileCount == 1) {
        "$senderAlias wants to send you a file"
    } else {
        "$senderAlias wants to send you $fileCount files"
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("Incoming files")
        .setContentText(contentText)
        .setSmallIcon(R.mipmap.ic_launcher_foreground)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .addAction(0, "Accept", acceptIntent)
        .addAction(0, "Decline", declineIntent)
        .build()

    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
}

private fun createReceiveRequestChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Incoming File Requests",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications for incoming file transfer requests"
            enableVibration(true)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
