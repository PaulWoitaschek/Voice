package de.ph1b.audiobook.mvp

import android.os.Bundle
import androidx.annotation.CallSuper
import de.ph1b.audiobook.misc.checkMainThread
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import timber.log.Timber

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

  private val compositeDisposable = CompositeDisposable()

  @CallSuper
  open fun onRestore(savedState: Bundle) {
    checkMainThread()
  }

  fun attach(view: V) {
    Timber.i("attach $view")
    checkMainThread()
    check(internalView == null) {
      "$internalView already bound."
    }

    internalView = view
    onAttachScope = MainScope()
    onAttach(view)
  }

  fun detach() {
    Timber.i("detach $internalView")
    checkMainThread()
    onAttachScope.cancel()
    compositeDisposable.clear()
    internalView = null
  }

  @CallSuper
  open fun onSave(state: Bundle) {
    checkMainThread()
  }

  open fun onAttach(view: V) {}
}
