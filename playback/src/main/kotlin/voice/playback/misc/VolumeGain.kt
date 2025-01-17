package voice.playback.misc

import android.media.audiofx.LoudnessEnhancer
import voice.logging.core.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.properties.Delegates

class VolumeGainSetter
@Inject constructor() {

  private var currentConfiguration: CurrentConfiguration? = null

  fun set(
    gain: Decibel,
    audioSession: Int,
  ) {
    Logger.v("set gain=$gain, session=$audioSession")
    if (gain == Decibel.Zero) {
      reset()
      Logger.v("Decibel=Zero. Detach the effect.")
      return
    }

    if (!updateCurrentConfiguration(audioSession, gain)) {
      createNewConfiguration(audioSession, gain)
    }
  }

  fun reset() {
    Logger.v("reset")
    currentConfiguration?.loudnessEnhancer?.release()
    currentConfiguration = null
  }

  private fun createNewConfiguration(
    audioSession: Int,
    gain: Decibel,
  ) {
    reset()

    val enhancer = createEnhancer(audioSession)
    if (enhancer != null) {
      enhancer.setGain(gain)
      currentConfiguration = CurrentConfiguration(enhancer, gain, audioSession)
      Logger.v("new configuration applied")
    } else {
      Logger.v("Could not apply new configuration.")
    }
  }

  private fun updateCurrentConfiguration(
    audioSession: Int,
    gain: Decibel,
  ): Boolean {
    val configuration = currentConfiguration ?: return false
    if (configuration.audioSession == audioSession) {
      if (configuration.gain != gain) {
        configuration.loudnessEnhancer.setGain(gain)
        currentConfiguration = configuration.copy(gain = gain)
      }
      Logger.v("configuration updated")
      return true
    } else {
      Logger.v("configuration not updated.")
      return false
    }
  }

  private fun LoudnessEnhancer.setGain(gain: Decibel) {
    try {
      setTargetGain(gain.milliBel)
    } catch (e: RuntimeException) {
      // throws random crashes. We catch and report them
      Logger.d(e)
    }
  }

  private fun createEnhancer(audioSessionId: Int): LoudnessEnhancer? {
    return try {
      LoudnessEnhancer(audioSessionId).apply {
        enabled = true
      }
    } catch (e: RuntimeException) {
      // the enhancer actually throws these oO
      Logger.d(e)
      return null
    }
  }

  private data class CurrentConfiguration(
    val loudnessEnhancer: LoudnessEnhancer,
    val gain: Decibel,
    val audioSession: Int,
  )
}

@Singleton
class VolumeGain
@Inject constructor(private val volumeGainSetter: VolumeGainSetter) {

  var gain: Decibel by Delegates.observable(Decibel(0F)) { _, _, _ -> updateLoudnessEnhancer() }
  var audioSessionId: Int? by Delegates.observable(null) { _, _, _ -> updateLoudnessEnhancer() }

  private fun updateLoudnessEnhancer() {
    Logger.v("updateLoudnessEnhancer(audioSessionId=$audioSessionId, gain=$gain")
    val audioSessionId = audioSessionId
    if (audioSessionId != null) {
      volumeGainSetter.set(gain, audioSessionId)
    } else {
      volumeGainSetter.reset()
    }
  }

  companion object {
    val MAX_GAIN = Decibel(9F)
  }
}
