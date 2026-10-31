package voice.core.playback.notification

import android.app.PendingIntent
import android.content.Intent

interface MainActivityIntentProvider {
  fun toCurrentBook(): PendingIntent
  fun currentBookIntent(): Intent
  fun libraryIntent(): Intent
}
