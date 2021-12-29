package voice.sleepTimer

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import timber.log.Timber
import kotlin.math.sqrt

class ShakeListener(private val onShake: () -> Unit) : SensorEventListener {

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

  override fun onSensorChanged(event: SensorEvent) {
    val gX = event.values[0] / SensorManager.GRAVITY_EARTH
    val gY = event.values[1] / SensorManager.GRAVITY_EARTH
    val gZ = event.values[2] / SensorManager.GRAVITY_EARTH
    val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)
    if (gForce > 2.25) {
      reportShakeDebounced()
    }
  }

  private var lastShakeAtMillis = 0L
  private fun reportShakeDebounced() {
    val currentMillis = System.currentTimeMillis()
    if (currentMillis - lastShakeAtMillis > 1000) {
      Timber.i("onShake")
      onShake()
      lastShakeAtMillis = currentMillis
    }
  }
}
