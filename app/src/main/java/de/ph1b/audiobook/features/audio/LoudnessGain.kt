package de.ph1b.audiobook.features.audio

import android.media.audiofx.LoudnessEnhancer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.properties.Delegates

/**
 * Handles the the loudness gain.
 */
@Singleton
class LoudnessGain @Inject constructor() {

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
    if (audioSessionId == -1)
      return

    try {
      if (effectWithSessionId == null) {
        val le = LoudnessEnhancer(audioSessionId).apply {
          enabled = true
        }
        effectWithSessionId = LoudnessEnhancerWithAudioSessionId(le, audioSessionId)
      }

      effectWithSessionId!!.loudnessEnhancer.setTargetGain(gainmB)
    } catch (e: RuntimeException) {
      // throws random crashes. We catch and report them
      Timber.e(e)
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
