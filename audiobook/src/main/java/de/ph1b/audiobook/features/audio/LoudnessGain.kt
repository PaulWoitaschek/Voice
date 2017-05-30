package de.ph1b.audiobook.features.audio

import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.properties.Delegates

/**
 * Handles the the loudness gain.
 *
 * @author Paul Woitaschek
 */
@Singleton class LoudnessGain @Inject constructor() {

  val supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
  var gainmB by Delegates.observable(0) { _, _, _ -> updateLoudnessEnhancer() }
  private var audioSessionId = -1

  private var effectWithSessionId: LoudnessEnhancerWithAudioSessionId? = null

  fun update(audioSessionId: Int) {
    this.audioSessionId = audioSessionId
    updateLoudnessEnhancer()
  }

  private fun updateLoudnessEnhancer() {
    if (gainmB == 0 || effectWithSessionId?.audioSessionId != audioSessionId) {
      detachEffect()
    }

    if (gainmB > 0) attachEffect()
  }

  private fun attachEffect() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      if (effectWithSessionId != null) {
        effectWithSessionId!!.loudnessEnhancer.setTargetGain(gainmB)
      } else {
        val le = LoudnessEnhancer(audioSessionId).apply {
          setTargetGain(gainmB)
          enabled = true
        }
        effectWithSessionId = LoudnessEnhancerWithAudioSessionId(le, audioSessionId)
      }
    }
  }

  private fun detachEffect() {
    effectWithSessionId?.loudnessEnhancer?.release()
    effectWithSessionId = null
  }

  private data class LoudnessEnhancerWithAudioSessionId(
      val loudnessEnhancer: LoudnessEnhancer,
      val audioSessionId: Int
  )

  companion object {
    const val MAX_MB = 900
  }
}
