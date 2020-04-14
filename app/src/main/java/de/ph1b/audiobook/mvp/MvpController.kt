package de.ph1b.audiobook.mvp

import android.os.Bundle
import android.view.View
import androidx.viewbinding.ViewBinding
import com.bluelinelabs.conductor.Controller
import de.ph1b.audiobook.features.InflateBinding
import de.ph1b.audiobook.features.ViewBindingController
import de.ph1b.audiobook.misc.checkMainThread

/**
 * Base controller that provides a convenient way for binding a view to a presenter
 */
abstract class MvpController<V : Any, out P, B : ViewBinding>(
  inflateBinding: InflateBinding<B>,
  args: Bundle = Bundle()
) : ViewBindingController<B>(args, inflateBinding) where P : Presenter<V> {

  private var internalPresenter: P? = null
  val presenter: P
    get() {
      checkMainThread()
      check(!isDestroyed) { "Must not call presenter when destroyed!" }
      if (internalPresenter == null) {
        internalPresenter = createPresenter()
      }
      return internalPresenter!!
    }

  init {
    addLifecycleListener(
      object : LifecycleListener {

        override fun onRestoreInstanceState(controller: Controller, savedInstanceState: Bundle) {
          presenter.onRestore(savedInstanceState)
        }

        override fun postAttach(controller: Controller, view: View) {
          presenter.attach(provideView())
        }

        override fun preDetach(controller: Controller, view: View) {
          presenter.detach()
        }

        override fun onSaveInstanceState(controller: Controller, outState: Bundle) {
          presenter.onSave(outState)
        }

        override fun postDestroy(controller: Controller) {
          internalPresenter = null
        }
      }
    )
  }

  @Suppress("UNCHECKED_CAST")
  open fun provideView(): V = this as V

  abstract fun createPresenter(): P
}
