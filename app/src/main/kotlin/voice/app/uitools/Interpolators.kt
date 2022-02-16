package voice.app.uitools

import android.view.animation.AccelerateInterpolator
import android.view.animation.Interpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Collection of interpolators
 */
object Interpolators {

  val fastOutSlowIn: Interpolator = FastOutSlowInInterpolator()
  val accelerate: Interpolator = AccelerateInterpolator()
}
