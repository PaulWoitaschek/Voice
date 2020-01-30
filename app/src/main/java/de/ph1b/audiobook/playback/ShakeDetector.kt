package de.ph1b.audiobook.playback

import android.hardware.Sensor
import android.hardware.SensorManager
import dagger.Reusable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

@Reusable
class ShakeDetector
@Inject constructor(private val sensorManager: SensorManager?) {

  fun shakeSupported() = sensorManager != null

  fun detect(): Flow<Unit> = callbackFlow {
    if (sensorManager == null) {
      return@callbackFlow
    }

    val listener = ShakeListener { offer(Unit) }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
    awaitClose {
      sensorManager.unregisterListener(listener)
    }
  }
}
