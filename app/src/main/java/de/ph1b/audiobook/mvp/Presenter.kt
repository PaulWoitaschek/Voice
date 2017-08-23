package de.ph1b.audiobook.mvp

import android.os.Bundle
import android.os.Looper
import android.support.annotation.CallSuper
import i
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

/**
 * Basic class for presenters that enables clients to control views offers handy ways for subscriptions.
 */
abstract class Presenter<V : Any> {

  val view: V
    get() {
      checkMainThread()
      return internalView!!
    }
  private var internalView: V? = null

  private val compositeDisposable = CompositeDisposable()

  @CallSuper
  open fun onRestore(savedState: Bundle) {
    checkMainThread()
  }

  fun attach(view: V) {
    checkMainThread()
    check(internalView == null) {
      "$internalView already bound."
    }

    i { "binding $view" }
    internalView = view
    onAttach(view)
  }

  fun detach() {
    checkMainThread()
    i { "Unbinding $view" }
    compositeDisposable.clear()
    internalView = null
  }

  @CallSuper
  open fun onSave(state: Bundle) {
    checkMainThread()
  }

  open fun onAttach(view: V) {}

  fun Disposable.disposeOnDetach() {
    checkMainThread()
    compositeDisposable.add(this)
  }

  private fun checkMainThread() {
    check(Looper.getMainLooper() == Looper.myLooper()) {
      "Is not on ui thread!"
    }
  }
}
