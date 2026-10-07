package voice.core.sleeptimer.impl

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.SensorBuilder
import org.robolectric.shadows.SensorEventBuilder
import voice.core.sleeptimer.ShakeDetectorImpl
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class ShakeDetectorImplTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val sensorManager = shadowOf(context.getSystemService<SensorManager>()!!)
  private val accelerometer = SensorBuilder.newBuilder().setType(Sensor.TYPE_ACCELEROMETER).build().also {
    sensorManager.addSensor(it)
  }
  private val shakeDetector = ShakeDetectorImpl(context)

  @Test
  fun `detect returns and stops listening once a shake is detected`() = runTest {
    val detection = launch { shakeDetector.detect() }
    runCurrent()
    assertTrue(sensorManager.getListeners().isNotEmpty())

    shake()
    runCurrent()

    assertTrue(detection.isCompleted)
    assertTrue(sensorManager.getListeners().isEmpty())
  }

  @Test
  fun `detect ignores the device lying still`() = runTest {
    val detection = launch { shakeDetector.detect() }
    runCurrent()

    shake(acceleration = SensorManager.GRAVITY_EARTH)
    runCurrent()

    assertFalse(detection.isCompleted)
    detection.cancel()
  }

  @Test
  fun `detect stops listening when cancelled`() = runTest {
    val detection = launch { shakeDetector.detect() }
    runCurrent()
    assertTrue(sensorManager.getListeners().isNotEmpty())

    detection.cancel()
    runCurrent()

    assertTrue(sensorManager.getListeners().isEmpty())
  }

  private fun shake(acceleration: Float = 20F) {
    // A shake is reported once most samples over at least 250ms exceed the acceleration threshold.
    repeat(4) { index ->
      val event = SensorEventBuilder.newBuilder(accelerometer, floatArrayOf(acceleration, 0F, 0F))
        .setTimestamp((index * 100).milliseconds.inWholeNanoseconds)
        .build()
      sensorManager.sendSensorEventToListeners(event)
    }
  }
}
