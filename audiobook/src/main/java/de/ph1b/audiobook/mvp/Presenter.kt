package de.ph1b.audiobook.mvp

import android.os.Bundle
import d
import i
import io.reactivex.disposables.CompositeDisposable


/**
 * Basic class for presenters that enables clients to control views offers handy ways for subscriptions.
 */
abstract class Presenter<V> {

  val view: V
    get() = internalView!!
  private var internalView: V? = null

  private var compositeDisposable: CompositeDisposable? = null

  open fun onRestore(savedState: Bundle) {

  }

  fun bind(view: V) {
    if (internalView == null) {
      i { "binding $view" }
      internalView = view

      compositeDisposable = CompositeDisposable()
      onBind(view, compositeDisposable!!)
    } else {
      d { "$view already bound" }
    }
  }

  fun unbind() {
    i { "Unbinding $view" }
    internalView = null
    compositeDisposable?.dispose()
  }

  open fun onSave(state: Bundle) {

  }

  abstract fun onBind(view: V, disposables: CompositeDisposable)
}
