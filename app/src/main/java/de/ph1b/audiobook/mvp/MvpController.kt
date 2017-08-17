package de.ph1b.audiobook.mvp

import android.databinding.ViewDataBinding
import android.os.Bundle
import android.view.View
import com.bluelinelabs.conductor.Controller
import de.ph1b.audiobook.features.BaseController

/**
 * Base fragment that provides a convenient way for binding a view to a presenter
 */
abstract class MvpController<V, out P, B>(args: Bundle = Bundle()) : BaseController<B>(args) where P : Presenter<V>, B : ViewDataBinding {

  private var internalPresenter: P? = null
  val presenter: P
    get() = if (isDestroyed) throw IllegalStateException("Must not call presenter when destroyed!")
    else {
      if (internalPresenter == null) {
        internalPresenter = createPresenter()
      }
      internalPresenter!!
    }

  init {
    @Suppress("LeakingThis")
    addLifecycleListener(object : LifecycleListener() {
      override fun onRestoreInstanceState(controller: Controller, savedInstanceState: Bundle) {
        presenter.onRestore(savedInstanceState)
      }

      override fun postAttach(controller: Controller, view: View) {
        presenter.bind(provideView())
      }

      override fun postDetach(controller: Controller, view: View) {
        presenter.unbind()
      }

      override fun onSaveInstanceState(controller: Controller, outState: Bundle) {
        presenter.onSave(outState)
      }

      override fun postDestroy(controller: Controller) {
        internalPresenter = null
      }
    })
  }

  @Suppress("UNCHECKED_CAST")
  open fun provideView(): V = this as V

  abstract fun createPresenter(): P
}
