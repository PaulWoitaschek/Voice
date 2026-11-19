package voice.core.playback.stats

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.collect.ImmutableList
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.AudioDeviceInfoBuilder

@RunWith(AndroidJUnit4::class)
class AudioOutputDetectorTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val audioManager = shadowOf(context.getSystemService(AudioManager::class.java))
  private val detector = AudioOutputDetector(context)

  private fun device(type: Int): AudioDeviceInfo =
    AudioDeviceInfoBuilder.newBuilder().setType(type).build()

  private fun soundGoesTo(vararg types: Int) {
    audioManager.setAudioDevicesForAttributes(
      AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build(),
      ImmutableList.copyOf(types.map(::device)),
    )
  }

  @Test
  fun `the phone speaker`() {
    soundGoesTo(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)

    assertEquals(expected = "speaker", actual = detector.current())
  }

  @Test
  fun `wired headphones, also on usb`() {
    listOf(
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
      )
      .forEach { type ->
        soundGoesTo(type)

        assertEquals(expected = "wired", actual = detector.current())
      }
  }

  @Test
  fun `bluetooth, also hearing aids`() {
    listOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_HEARING_AID,
      )
      .forEach { type ->
        soundGoesTo(type)

        assertEquals(expected = "bluetooth", actual = detector.current())
      }
  }

  @Test
  fun `an output that voice can't tell is other`() {
    soundGoesTo(AudioDeviceInfo.TYPE_HDMI)

    assertEquals(expected = "other", actual = detector.current())
  }

  @Test
  fun `no output is other`() {
    soundGoesTo()

    assertEquals(expected = "other", actual = detector.current())
  }

  @Test
  @Config(sdk = [32])
  fun `before android 13, bluetooth that is connected wins over the phone speaker`() {
    audioManager.setOutputDevices(
      listOf(
        device(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE),
        device(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER),
        device(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP),
      ),
    )

    assertEquals(expected = "bluetooth", actual = detector.current())
  }

  @Test
  @Config(sdk = [32])
  fun `before android 13, a bluetooth device that only does calls is not the output`() {
    audioManager.setOutputDevices(
      listOf(
        device(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER),
        device(AudioDeviceInfo.TYPE_BLUETOOTH_SCO),
      ),
    )

    assertEquals(expected = "speaker", actual = detector.current())
  }

  @Test
  @Config(sdk = [32])
  fun `before android 13, the phone speaker when nothing else is connected`() {
    audioManager.setOutputDevices(
      listOf(
        device(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE),
        device(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER),
      ),
    )

    assertEquals(expected = "speaker", actual = detector.current())
  }
}
