package de.ph1b.audiobook.playback

import android.hardware.Sensor
import android.hardware.SensorManager
import dagger.Reusable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import javax.inject.Inject

@Suppress("EXPERIMENTAL_API_USAGE")
@Reusable
class ShakeDetector
@Inject constructor(private val sensorManager: SensorManager?) {

  fun shakeSupported() = sensorManager != null

  fun detect(): ReceiveChannel<Unit> {
    return Channel<Unit>(Channel.CONFLATED).apply {
      if (sensorManager == null) {
        close()
        return@apply
      }
      val listener = ShakeListener { offer(Unit) }
      val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
      sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
      invokeOnClose {
        sensorManager.unregisterListener(listener)
      }
    }
  }
}
