package voice.sleepTimer

import android.content.Context
import android.hardware.SensorManager
import dagger.Reusable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import javax.inject.Inject
import com.squareup.seismic.ShakeDetector as SeismicShakeDetector

@Reusable
class ShakeDetector
@Inject constructor(private val context: Context) {

  /**
   * This function returns once a shake was detected
   */
  suspend fun detect() {
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
      ?: awaitCancellation()
    val shakeDetected = CompletableDeferred<Unit>()
    val listener = SeismicShakeDetector.Listener {
      shakeDetected.complete(Unit)
    }
    val shakeDetector = SeismicShakeDetector(listener)
    try {
      shakeDetector.start(sensorManager)
      shakeDetected.await()
    } finally {
      shakeDetector.stop()
    }
  }
}
