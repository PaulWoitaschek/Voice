package de.ph1b.audiobook.features.audio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import i
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The equalizer. Delegates to the system integrated equalizer
 *
 * @author Paul Woitaschek
 */
@Singleton class Equalizer @Inject constructor(
    private val context: Context
) {

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

  private var audioSessionId: Int? = null

  fun update(audioSessionId: Int) {
    i { "update to $audioSessionId" }
    if (audioSessionId == -1) return
    if (this.audioSessionId != audioSessionId) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        val le = LoudnessEnhancer(audioSessionId)
        le.setTargetGain(600)
        le.enabled = true
        le.release()
      }
      this.audioSessionId = audioSessionId
    }
    updateIntent.putAudioSessionId(audioSessionId)
    launchIntent.putAudioSessionId(audioSessionId)
    context.sendBroadcast(updateIntent)
  }

  fun launch(activity: Activity) {
    i { "launch" }
    activity.startActivityForResult(launchIntent, 12)
  }
}
