package de.ph1b.audiobook.playback.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import de.ph1b.audiobook.R

/**
 * Creates the notification channel and exposes the channel name.
 */
class NotificationChannelCreator(
  notificationManager: NotificationManager,
  context: Context
) {

  val musicChannel = "musicChannel4"

  init {
    createChannel(context, notificationManager)
  }

  @SuppressLint("NewApi")
  private fun createChannel(context: Context, notificationManager: NotificationManager) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = context.getString(R.string.music_notification)
      val channel = NotificationChannel(
        musicChannel,
        name,
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        setShowBadge(false)
      }
      notificationManager.createNotificationChannel(channel)
    }
  }
}
