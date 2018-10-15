package de.ph1b.audiobook.misc

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.RestoreViewOnCreateController
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler

private const val SI_DIALOG = "android:savedDialogState"

/**
 * A wrapper that wraps a dialog in a controller
 */
abstract class DialogController(args: Bundle = Bundle()) : RestoreViewOnCreateController(args) {

  private var dialog: Dialog? = null
  private var dismissed = false

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedViewState: Bundle?
  ): View {
    dialog = onCreateDialog(savedViewState).apply {
      ownerActivity = activity!!
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

  override fun onSaveViewState(view: View, outState: Bundle) {
    super.onSaveViewState(view, outState)
    val dialogState = dialog!!.onSaveInstanceState()
    outState.putBundle(SI_DIALOG, dialogState)
  }

  override fun onAttach(view: View) {
    super.onAttach(view)
    dialog!!.show()
  }

  override fun onDetach(view: View) {
    super.onDetach(view)
    dialog!!.hide()
  }

  override fun onDestroyView(view: View) {
    super.onDestroyView(view)
    dialog!!.setOnDismissListener(null)
    dialog!!.dismiss()
    dialog = null
  }

  fun showDialog(router: Router, tag: String? = null) {
    dismissed = false
    router.pushController(
      RouterTransaction.with(this)
        .pushChangeHandler(SimpleSwapChangeHandler(false))
        .tag(tag)
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
