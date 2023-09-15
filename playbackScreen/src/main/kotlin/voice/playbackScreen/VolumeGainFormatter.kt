package voice.playbackScreen

import voice.playback.misc.Decibel
import java.text.DecimalFormat
import javax.inject.Inject

class VolumeGainFormatter
@Inject constructor() {

  private val dbFormat = DecimalFormat("0.0 dB")

  fun format(gain: Decibel): String {
    return dbFormat.format(gain.value)
  }
}
