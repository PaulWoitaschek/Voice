package voice.app.uitools

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.transition.ArcMotion
import androidx.transition.ChangeBounds
import androidx.transition.ChangeClipBounds
import androidx.transition.ChangeImageTransform
import androidx.transition.ChangeTransform
import androidx.transition.Transition
import androidx.transition.TransitionSet
import com.bluelinelabs.conductor.changehandler.androidxtransition.TransitionChangeHandler
import voice.app.R

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
      .addTransition(
        ChangeBounds().apply {
          setPathMotion(ArcMotion())
        }
      )
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

    return TransitionSet()
      .addTransition(moveFabAndCover)
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
