package de.ph1b.audiobook.uitools

import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.ChangeClipBounds
import android.transition.ChangeImageTransform
import android.transition.ChangeTransform
import android.transition.Fade
import android.transition.Transition
import android.transition.TransitionSet
import android.view.View
import android.view.ViewGroup
import androidx.view.isVisible
import com.bluelinelabs.conductor.changehandler.TransitionChangeHandler
import de.ph1b.audiobook.R

/**
 * Transition from book list to book details
 */
private const val SI_TRANSITION_NAME = "niTransitionName"

class BookChangeHandler : TransitionChangeHandler() {

  var transitionName: String? = null

  override fun getTransition(
    container: ViewGroup,
    from: View?,
    to: View?,
    isPush: Boolean
  ): Transition {
    val moveFabAndCover = TransitionSet()
      .addTransition(ChangeBounds())
      .addTransition(ChangeTransform())
      .addTransition(ChangeClipBounds())
      .addTransition(ChangeImageTransform())
      .addTarget(R.id.fab)
      .addTarget(R.id.play)
      .excludeTarget(R.id.toolbar, true)
      .addTarget(R.id.cover)
      .apply {
        transitionName?.let { addTarget(it) }
      }
      .setDuration(250)

    val fadeGradient = Fade()
      .addTarget(R.id.gradientShadow)
      .setDuration(100)

    if (!isPush && from != null) {
      val gradient = from.findViewById<View?>(R.id.gradientShadow)
      gradient?.isVisible = false
    }

    val moveFade = TransitionSet()
      .addTransition(moveFabAndCover)
      .addTransition(fadeGradient)

    return if (isPush && to != null) {
      val circularTransition = CircularRevealBookPlayTransition()
        .apply {
          duration = 450
          addTarget(R.id.previous)
          addTarget(R.id.next)
          addTarget(R.id.fastForward)
          addTarget(R.id.rewind)
        }

      TransitionSet()
        .setOrdering(TransitionSet.ORDERING_SEQUENTIAL)
        .addTransition(moveFade)
        .addTransition(circularTransition)
    } else moveFade
  }

  override fun saveToBundle(bundle: Bundle) {
    super.saveToBundle(bundle)
    bundle.putString(SI_TRANSITION_NAME, transitionName)
  }

  override fun restoreFromBundle(bundle: Bundle) {
    super.restoreFromBundle(bundle)
    transitionName = bundle.getString(SI_TRANSITION_NAME)
  }
}
