package de.ph1b.audiobook.uitools

import android.animation.Animator
import android.animation.AnimatorSet
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.changehandler.AnimatorChangeHandler

/**
 * Change handler that animates vertically
 */
class VerticalChangeHandler : AnimatorChangeHandler() {

  override fun resetFromView(from: View) {
  }

  override fun getAnimator(
    container: ViewGroup,
    from: View?,
    to: View?,
    isPush: Boolean,
    toAddedToContainer: Boolean
  ): Animator {
    return if (isPush && to != null) {
      animateFloat(to.height / 2F, 0F) { value, fraction ->
        to.translationY = value
        to.alpha = fraction
      }.apply { interpolator = Interpolators.fastOutSlowIn }
    } else if (!isPush && from != null) {
      animateFloat(0F, from.height / 2F) { value, fraction ->
        from.translationY = value
        from.alpha = 1 - fraction
      }.apply {
        interpolator = Interpolators.accelerate
        duration =
          from.context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
      }
    } else AnimatorSet()
  }
}
