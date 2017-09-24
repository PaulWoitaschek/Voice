package de.ph1b.audiobook.uitools

import android.animation.Animator
import android.animation.AnimatorSet
import android.transition.Transition
import android.transition.TransitionValues
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import de.ph1b.audiobook.R

private const val VISIBILITIES = "de.ph1b.audiobook#circularRevealTransition#visibilities"

class CircularRevealBookPlayTransition : Transition() {

  override fun captureStartValues(values: TransitionValues) {}

  override fun captureEndValues(values: TransitionValues) {
    val views = findViews(values.view)
    val visibilities = views.visibilities()
    values.values.put(VISIBILITIES, visibilities)
  }

  override fun createAnimator(root: ViewGroup, startValues: TransitionValues?, endValues: TransitionValues?): Animator {
    val view = endValues?.view
        ?: return AnimatorSet()

    val width = view.width
    val cx = width / 2
    val finalRadius = width.toFloat()
    val views = findViews(endValues.view)

    val animators = circularRevealAnimators(cx, finalRadius, views.previous, views.rewind, views.fastForward, views.next)

    animators.first()
        .addListener(object : DefaultAnimatorListener {
          override fun onAnimationStart(animator: Animator) {
            val visibilities = endValues.values[VISIBILITIES] as Visibilities
            views.apply(visibilities)
          }
        })

    return AnimatorSet()
        .apply {
          playTogether(*animators.toTypedArray())
          addListener(object : DefaultAnimatorListener {
            override fun onAnimationStart(animator: Animator) {
              views.apply(Visibilities.INVISIBLE)
            }
          })
        }
  }

  private fun circularRevealAnimators(cx: Int, finalRadius: Float, vararg targets: View): List<Animator> = targets.map {
    circularRevealAnimator(it, cx, finalRadius)
  }

  private fun circularRevealAnimator(target: View, cx: Int, finalRadius: Float): Animator =
      ViewAnimationUtils.createCircularReveal(
          target,
          cx - target.left,
          target.height / 2,
          0f,
          finalRadius
      ).apply {
        startDelay = 150
      }

  private fun findViews(root: View) = Views(
      previous = root.findViewById(R.id.previous),
      rewind = root.findViewById(R.id.rewind),
      fastForward = root.findViewById(R.id.fastForward),
      next = root.findViewById(R.id.next)
  )

  private data class Views(
      val previous: View,
      val rewind: View,
      val fastForward: View,
      val next: View
  ) {

    fun visibilities() = Visibilities(
        previous = previous.visibility,
        rewind = rewind.visibility,
        fastForward = fastForward.visibility,
        next = next.visibility
    )

    fun apply(visibilities: Visibilities) {
      previous.visibility = visibilities.previous
      rewind.visibility = visibilities.rewind
      fastForward.visibility = visibilities.fastForward
      next.visibility = visibilities.next
    }
  }

  private data class Visibilities(
      val previous: Int,
      val rewind: Int,
      val fastForward: Int,
      val next: Int
  ) {

    companion object {
      val INVISIBLE = Visibilities(View.INVISIBLE, View.INVISIBLE, View.INVISIBLE, View.INVISIBLE)
    }
  }
}
