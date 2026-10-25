package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AlertType

object NotificationHelper {
  const val CHANNEL_ID = "ecocontrol_system_alerts"
  const val CHANNEL_NAME = "EcoControl Alerts"
  const val CHANNEL_DESC = "Notifications for environmental thresholds, dry soil, high temperatures, rain, and device status"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
        description = CHANNEL_DESC
        enableVibration(true)
        enableLights(true)
      }
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun sendAlertNotification(
    context: Context,
    id: Int,
    title: String,
    message: String,
    alertType: AlertType
  ) {
    createNotificationChannel(context)

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent: PendingIntent = PendingIntent.getActivity(
      context,
      id,
      intent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val iconRes = when (alertType) {
      AlertType.DRY_SOIL -> android.R.drawable.stat_sys_warning
      AlertType.HIGH_TEMP -> android.R.drawable.stat_notify_error
      AlertType.RAIN -> android.R.drawable.stat_sys_download_done
      AlertType.LOW_BATTERY -> android.R.drawable.stat_notify_error
      AlertType.DEVICE_OFFLINE -> android.R.drawable.stat_sys_warning
      AlertType.SYSTEM_INFO -> android.R.drawable.stat_notify_chat
    }

    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(iconRes)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)

    try {
      with(NotificationManagerCompat.from(context)) {
        notify(id, builder.build())
      }
    } catch (_: SecurityException) {
      // User hasn't granted POST_NOTIFICATIONS permission
    }
  }

  fun sendTestNotification(context: Context) {
    sendAlertNotification(
      context = context,
      id = 9999,
      title = "EcoControl Alert Test",
      message = "Notification system is active and monitoring environmental thresholds in real-time.",
      alertType = AlertType.SYSTEM_INFO
    )
  }
}
