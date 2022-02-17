package voice.app.mvp

import android.os.Bundle
import androidx.annotation.CallSuper
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import voice.common.checkMainThread
import voice.logging.core.Logger

abstract class Presenter<V : Any> {

  protected val scope = MainScope()
  protected var onAttachScope = MainScope().also { it.cancel() }

  val view: V
    get() {
      checkMainThread()
      return internalView!!
    }
  val attached: Boolean
    get() {
      checkMainThread()
      return internalView != null
    }

  private var internalView: V? = null

  @CallSuper
  open fun onRestore(savedState: Bundle) {
    checkMainThread()
  }

  fun attach(view: V) {
    Logger.i("attach $view")
    checkMainThread()
    check(internalView == null) {
      "$internalView already bound."
    }

    internalView = view
    onAttachScope = MainScope()
    onAttach(view)
  }

  fun detach() {
    Logger.i("detach $internalView")
    checkMainThread()
    onAttachScope.cancel()
    internalView = null
  }

  @CallSuper
  open fun onSave(state: Bundle) {
    checkMainThread()
  }

  open fun onAttach(view: V) {}
}
