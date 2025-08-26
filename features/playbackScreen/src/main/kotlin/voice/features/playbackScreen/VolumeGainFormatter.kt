package voice.features.playbackScreen

import dev.zacsweers.metro.Inject
import voice.core.playback.misc.Decibel
import java.text.DecimalFormat

@Inject
class VolumeGainFormatter {

  private val dbFormat = DecimalFormat("0.0 dB")

  fun format(gain: Decibel): String {
    return dbFormat.format(gain.value)
  }
}
