package de.ph1b.audiobook.mvp

import android.os.Bundle
import android.support.annotation.CallSuper
import android.view.View
import de.ph1b.audiobook.features.BaseController

/**
 * Base fragment that provides a convenient way for binding a view to a presenter
 *
 * @author Paul Woitaschek
 */
abstract class MvpBaseController<V, P> : BaseController() where P : Presenter<V> {

    abstract fun newPresenter(): P

    abstract fun provideView(): V

    val presenter = newPresenter()

    @CallSuper override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        presenter.onRestore(savedInstanceState)
    }

    @CallSuper override fun onAttach(view: View) {
        presenter.bind(provideView())
    }

    @CallSuper override fun onDetach(view: View) {
        presenter.unbind()
    }

    @CallSuper override fun onSaveInstanceState(outState: Bundle) {
        presenter.onSave(outState)
    }
}