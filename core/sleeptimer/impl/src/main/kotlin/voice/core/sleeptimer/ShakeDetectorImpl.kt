package voice.core.sleeptimer

import android.content.Context
import android.hardware.SensorManager
import androidx.core.content.getSystemService
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import com.squareup.seismic.ShakeDetector as SeismicShakeDetector

@ContributesBinding(AppScope::class)
class ShakeDetectorImpl(private val context: Context) : ShakeDetector {

  override suspend fun detect() {
    val sensorManager = context.getSystemService<SensorManager>() ?: awaitCancellation()
    val shaken = CompletableDeferred<Unit>()
    val listener = SeismicShakeDetector.Listener { shaken.complete(Unit) }
    val shakeDetector = SeismicShakeDetector(listener)
    shakeDetector.start(sensorManager, SensorManager.SENSOR_DELAY_GAME)
    try {
      shaken.await()
    } finally {
      shakeDetector.stop()
    }
  }
}
