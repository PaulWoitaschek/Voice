package de.ph1b.audiobook.mvp

import android.os.Bundle

/**
 * Delegates lifecycle methods to the presenter.
 *
 * @author Paul Woitaschek
 */
class PresenterDelegate<V, out P>(private val newPresenter: () -> P, private val getView: () -> V)  where P : Presenter<V> {

  private var presenter: P? = null
    private set

  fun presenter() = presenter!!

  fun onCreate(savedInstanceState: Bundle?) {
    presenter = newPresenter.invoke()
    if (savedInstanceState != null) presenter!!.onRestore(savedInstanceState)
  }

  fun onStart() {
    presenter!!.bind(getView.invoke())
  }

  fun onStop() {
    presenter!!.unbind()
  }

  fun onSaveInstanceState(outState: Bundle) {
    presenter!!.onSave(outState)
  }

  fun onDestroy() {
    presenter = null
  }
}