package de.ph1b.audiobook.misc

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler
import de.ph1b.audiobook.misc.conductor.ControllerLifecycleOwner
import de.ph1b.audiobook.misc.conductor.LifecycleScopeProperty

private const val SI_DIALOG = "android:savedDialogState"

/**
 * A wrapper that wraps a dialog in a controller
 */
abstract class DialogController(args: Bundle = Bundle()) : Controller(args), LifecycleOwner {

  @Suppress("LeakingThis")
  private val lifecycleOwner = ControllerLifecycleOwner(this)

  override fun getLifecycle(): Lifecycle = lifecycleOwner.lifecycle

  val lifecycleScope by LifecycleScopeProperty(lifecycle)

  private var dialog: Dialog? = null
  private var dismissed = false

  final override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedViewState: Bundle?
  ): View {
    dialog = onCreateDialog(savedViewState).apply {
      setOwnerActivity(activity!!)
      setOnDismissListener { dismissDialog() }
      if (savedViewState != null) {
        val dialogState = savedViewState.getBundle(SI_DIALOG)
        if (dialogState != null) {
          onRestoreInstanceState(dialogState)
        }
      }
    }
    // stub view
    return View(activity)
  }

  @CallSuper
  override fun onSaveViewState(view: View, outState: Bundle) {
    super.onSaveViewState(view, outState)
    val dialogState = dialog!!.onSaveInstanceState()
    outState.putBundle(SI_DIALOG, dialogState)
  }

  @CallSuper
  override fun onAttach(view: View) {
    super.onAttach(view)
    dialog!!.show()
  }

  @CallSuper
  override fun onDetach(view: View) {
    super.onDetach(view)
    dialog!!.hide()
  }

  @CallSuper
  override fun onDestroyView(view: View) {
    super.onDestroyView(view)
    dialog!!.setOnDismissListener(null)
    dialog!!.dismiss()
    dialog = null
  }

  fun showDialog(router: Router) {
    dismissed = false
    router.pushController(
      RouterTransaction.with(this)
        .pushChangeHandler(SimpleSwapChangeHandler(false))
    )
  }

  fun dismissDialog() {
    if (dismissed) {
      return
    }
    router.popController(this)
    dismissed = true
  }

  protected abstract fun onCreateDialog(savedViewState: Bundle?): Dialog
}
