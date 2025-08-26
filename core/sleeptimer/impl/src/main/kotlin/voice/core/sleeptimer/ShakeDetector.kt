package voice.core.sleeptimer

import android.content.Context
import android.hardware.SensorManager
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.squareup.seismic.ShakeDetector as SeismicShakeDetector

@Inject
class ShakeDetector(private val context: Context) {

  /**
   * This function returns once a shake was detected
   */
  suspend fun detect() {
    suspendCancellableCoroutine { cont ->
      val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager?
        ?: return@suspendCancellableCoroutine
      val listener = SeismicShakeDetector.Listener {
        if (!cont.isCompleted) {
          cont.resume(Unit)
        }
      }
      val shakeDetector = SeismicShakeDetector(listener)
      shakeDetector.start(sensorManager, SensorManager.SENSOR_DELAY_GAME)
      cont.invokeOnCancellation {
        shakeDetector.stop()
      }
    }
  }
}
