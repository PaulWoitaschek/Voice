package de.ph1b.audiobook.features.bookPlaying

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import com.google.android.exoplayer2.C
import i
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The equalizer. Delegates to the system integrated equalizer
 *
 * @author Paul Woitaschek
 */
@Singleton class Equalizer @Inject constructor(private val context: Context) {

  private val launchIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
  }
  private val updateIntent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
  }

  val exists = context.packageManager.resolveActivity(launchIntent, 0) != null

  private fun Intent.putAudioSessionId(audioSessionId: Int) {
    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
  }

  fun update(audioSessionId: Int) {
    i { "update to $audioSessionId" }

    if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
    updateIntent.putAudioSessionId(audioSessionId)
    context.sendBroadcast(updateIntent)
  }

  fun launch(activity: Activity, audioSessionId: Int) {
    i { "launch with $audioSessionId" }
    if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return

    launchIntent.putAudioSessionId(audioSessionId)
    activity.startActivityForResult(launchIntent, 12)
  }
}

