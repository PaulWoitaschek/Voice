package de.ph1b.audiobook.features.audio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import timber.log.Timber

/**
 * The equalizer. Delegates to the system integrated equalizer
 */
class Equalizer(private val context: Context) {

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
    Timber.i("update to $audioSessionId")
    if (audioSessionId == -1)
      return
    updateIntent.putAudioSessionId(audioSessionId)
    launchIntent.putAudioSessionId(audioSessionId)
    context.sendBroadcast(updateIntent)
  }

  fun launch(activity: Activity) {
    Timber.i("launch")
    activity.startActivityForResult(launchIntent, 12)
  }
}
