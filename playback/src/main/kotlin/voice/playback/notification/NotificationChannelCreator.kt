package voice.playback.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import voice.playback.R
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

const val MUSIC_CHANNEL_ID = "musicChannel4"

@Singleton
class NotificationChannelCreator
@Inject constructor(
  private val notificationManager: NotificationManager,
  private val context: Context
) {

  private var created = AtomicBoolean()

  fun createChannel() {
    if (created.getAndSet(true)) {
      return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = context.getString(R.string.music_notification)
      val channel = NotificationChannel(
        MUSIC_CHANNEL_ID,
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
