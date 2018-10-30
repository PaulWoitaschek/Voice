package de.ph1b.audiobook.playback

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.Reusable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.channels.produce
import javax.inject.Inject

/**
 * Observable for detecting shakes. The onSensorChanged formula was taken from
 * https://github.com/AntennaPod/AntennaPod/blob/8d2ec19cbe05297afa887cc2263347f112aae3e6/core/src/main/java/de/danoeh/antennapod/core/service/playback/ShakeListener.java
 * And is licensesd as apache 2
 */
@Suppress("EXPERIMENTAL_API_USAGE")
@Reusable
class ShakeDetector
@Inject constructor(private val sensorManager: SensorManager?) {

  fun shakeSupported() = sensorManager != null

  fun CoroutineScope.detect(): ReceiveChannel<Unit> {
    return produce {
      if (sensorManager == null) {
        close()
        return@produce
      }
      val resultChannel = Channel<Unit>(Channel.CONFLATED)
      val listener = ShakeListener {
        resultChannel.offer(Unit)
      }
      val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
      sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
      try {
        resultChannel.consumeEach {
          send(it)
        }
      } finally {
        sensorManager.unregisterListener(listener)
      }
    }
  }

  private class ShakeListener(private val onShake: () -> Unit) : SensorEventListener {

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
      val gX = event.values[0] / SensorManager.GRAVITY_EARTH
      val gY = event.values[1] / SensorManager.GRAVITY_EARTH
      val gZ = event.values[2] / SensorManager.GRAVITY_EARTH

      val gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ.toDouble())
      if (gForce > 2.25) {
        onShake()
      }
    }
  }
}
