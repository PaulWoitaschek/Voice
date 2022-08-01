package voice.app.uitools

import android.animation.ValueAnimator

/** simplifies value animation */
fun animateFloat(
  vararg values: Float,
  action: (value: Float, fraction: Float) -> Unit,
): ValueAnimator {
  val valueAnimator = ValueAnimator()
  valueAnimator.setFloatValues(*values)
  valueAnimator.addUpdateListener {
    val animatedValue = it.animatedValue as Float
    action(animatedValue, it.animatedFraction)
  }
  return valueAnimator
}
