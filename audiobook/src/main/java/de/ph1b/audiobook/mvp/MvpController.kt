package de.ph1b.audiobook.mvp

import android.databinding.ViewDataBinding
import android.os.Bundle
import android.view.View
import com.bluelinelabs.conductor.Controller
import de.ph1b.audiobook.features.BaseController

/**
 * Base fragment that provides a convenient way for binding a view to a presenter
 *
 * @author Paul Woitaschek
 */
abstract class MvpController<V, out P, B>(args: Bundle) : BaseController<B>(args) where P : Presenter<V>, B : ViewDataBinding {

  constructor() : this(Bundle())

  init {
    @Suppress("LeakingThis")
    addLifecycleListener(object : LifecycleListener() {
      override fun onRestoreInstanceState(controller: Controller, savedInstanceState: Bundle) {
        if (internalPresenter == null) internalPresenter = createPresenter()
        presenter.onRestore(savedInstanceState)
      }

      override fun postAttach(controller: Controller, view: View) {
        if (internalPresenter == null) internalPresenter = createPresenter()
        presenter.bind(provideView())
      }

      override fun postDetach(controller: Controller, view: View) {
        presenter.unbind()
      }

      override fun onSaveInstanceState(controller: Controller, outState: Bundle) {
        presenter.onSave(outState)
      }
    })
  }

  @Suppress("UNCHECKED_CAST")
  open fun provideView(): V = this as V

  val presenter: P
    get() = internalPresenter!!

  private var internalPresenter: P? = null

  abstract fun createPresenter(): P
}
