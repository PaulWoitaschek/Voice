package de.ph1b.audiobook.uitools

import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Interpolator

/**
 * Collection of interpolators
 */
object Interpolators {
  val fastOutSlowIn: Interpolator = FastOutSlowInInterpolator()
  val accelerate: Interpolator = AccelerateInterpolator()
}
