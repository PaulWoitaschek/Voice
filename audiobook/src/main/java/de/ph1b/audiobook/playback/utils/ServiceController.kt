package de.ph1b.audiobook.playback.utils

import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import de.ph1b.audiobook.playback.PlaybackService
import javax.inject.Inject

/**
 * Providing intents that can be sent to the playback service.
 *
 * @author Paul Woitaschek
 */
class ServiceController @Inject constructor(private val context: Context) {

  private fun intent() = Intent(context, PlaybackService::class.java)

  private fun mediaButtonIntent(event: Int) = intent().apply {
    action = Intent.ACTION_MEDIA_BUTTON
    val keyEvent = KeyEvent(KeyEvent.ACTION_DOWN, event)
    putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
  }

  fun getStopIntent() = mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_STOP)

  fun getPlayPauseIntent() = mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)

  fun getFastForwardIntent() = mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)

  fun getRewindIntent() = mediaButtonIntent(KeyEvent.KEYCODE_MEDIA_REWIND)
}
