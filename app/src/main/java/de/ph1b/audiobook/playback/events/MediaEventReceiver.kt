package de.ph1b.audiobook.playback.events

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startForegroundService
import de.ph1b.audiobook.playback.PlaybackService

/**
 * Forwards intents to [PlaybackService]
 */
class MediaEventReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context?, intent: Intent?) {
    if (context != null && intent != null) {
      val playerIntent = Intent(intent).apply {
        component = ComponentName(context, PlaybackService::class.java)
      }
      startForegroundService(context, playerIntent)
    }
  }
}
