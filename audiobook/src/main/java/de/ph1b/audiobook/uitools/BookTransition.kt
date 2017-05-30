package de.ph1b.audiobook.uitools

import android.annotation.TargetApi
import android.os.Bundle
import android.transition.Transition
import android.transition.TransitionInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.changehandler.TransitionChangeHandler
import de.ph1b.audiobook.R

/**
 * Transition from book list to book details
 *
 * @author Paul Woitaschek
 */
@TargetApi(21)
class BookTransition : TransitionChangeHandler() {

  var transitionName: String? = null

  override fun getTransition(container: ViewGroup, from: View?, to: View?, isPush: Boolean): Transition {
    val context = container.context
    val move = TransitionInflater.from(context)
        .inflateTransition(android.R.transition.move)
    move.addTarget(R.id.fab)
    move.addTarget(R.id.play)
    move.excludeTarget(R.id.toolbar, true)
    if (transitionName != null) move.addTarget(transitionName)
    move.addTarget(R.id.cover)
    return move
  }

  override fun saveToBundle(bundle: Bundle) {
    super.saveToBundle(bundle)
    bundle.putString(SI_TRANSITION_NAME, transitionName)
  }

  override fun restoreFromBundle(bundle: Bundle) {
    super.restoreFromBundle(bundle)
    transitionName = bundle.getString(SI_TRANSITION_NAME)
  }

  companion object {
    private val SI_TRANSITION_NAME = "niTransitionName"
  }
}
