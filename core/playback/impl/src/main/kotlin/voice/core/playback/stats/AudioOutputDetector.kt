package voice.core.playback.stats

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import dev.zacsweers.metro.Inject
import voice.core.logging.api.Logger

@Inject
class AudioOutputDetector(private val context: Context) {

  fun current(): String =
    try {
      val audioManager = context.getSystemService(AudioManager::class.java)
      val devices =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          audioManager.getAudioDevicesForAttributes(mediaAttributes)
        } else {
          // older versions only tell what is connected, and Android plays on these in this order
          audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()
        }
      val types = devices.map { it.type }
      when {
        types.any { it in bluetooth } -> "bluetooth"
        types.any { it in wired } -> "wired"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER in types -> "speaker"
        else -> OTHER
      }
    } catch (e: Exception) {
      Logger.w(e, "Could not tell the audio output")
      OTHER
    }

  companion object {
    const val OTHER = "other"

    private val mediaAttributes: AudioAttributes =
      AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()

    // without the call profile: a watch or a car kit that only does calls plays no book
    private val bluetooth =
      setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_BLE_BROADCAST,
        AudioDeviceInfo.TYPE_HEARING_AID,
      )

    private val wired =
      setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
      )
  }
}
