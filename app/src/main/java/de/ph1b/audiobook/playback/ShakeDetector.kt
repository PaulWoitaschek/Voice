package de.ph1b.audiobook.playback

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.reactivex.Observable

/**
 * Observable for detecting shakes. The onSensorChanged formula was taken from
 * https://github.com/AntennaPod/AntennaPod/blob/8d2ec19cbe05297afa887cc2263347f112aae3e6/core/src/main/java/de/danoeh/antennapod/core/service/playback/ShakeListener.java
 * And is licensesd as apache 2
 */
class ShakeDetector(context: Context) {

  val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
  fun shakeSupported() = sensorManager != null

  fun detect(): Observable<Unit> = Observable.create {
    if (sensorManager == null) return@create

    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val listener = object : SensorEventListener {
      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
      }

      override fun onSensorChanged(event: SensorEvent) {
        val gX = event.values[0] / SensorManager.GRAVITY_EARTH
        val gY = event.values[1] / SensorManager.GRAVITY_EARTH
        val gZ = event.values[2] / SensorManager.GRAVITY_EARTH

        val gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ.toDouble())
        if (gForce > 2.25) {
          it.onNext(Unit)
        }
      }
    }

    // subscribed upon registration, unsubscribe upon cancellation
    sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
    it.setCancellable { sensorManager.unregisterListener(listener) }
  }
}
