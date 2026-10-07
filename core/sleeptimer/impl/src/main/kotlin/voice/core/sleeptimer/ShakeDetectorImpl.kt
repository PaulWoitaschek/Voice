package voice.core.sleeptimer

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlin.time.Duration.Companion.nanoseconds

@ContributesBinding(AppScope::class)
class ShakeDetectorImpl(private val context: Context) : ShakeDetector {

  override suspend fun detect() {
    val sensorManager = context.getSystemService<SensorManager>() ?: awaitCancellation()
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: awaitCancellation()
    val shaken = CompletableDeferred<Unit>()
    val shakeWindow = ShakeWindow()
    val listener = object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent) {
        if (shakeWindow.add(event.timestamp.nanoseconds, event.isAccelerating())) {
          shaken.complete(Unit)
        }
      }

      override fun onAccuracyChanged(
        sensor: Sensor,
        accuracy: Int,
      ) {}
    }
    sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
    try {
      shaken.await()
    } finally {
      sensorManager.unregisterListener(listener)
    }
  }
}

// In m/s², including gravity
private const val ACCELERATION_THRESHOLD = 13F

private fun SensorEvent.isAccelerating(): Boolean {
  val (x, y, z) = values
  return x * x + y * y + z * z > ACCELERATION_THRESHOLD * ACCELERATION_THRESHOLD
}
