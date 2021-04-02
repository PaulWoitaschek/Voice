package de.ph1b.audiobook.common.conductor

import android.content.Context
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.Controller.LifecycleListener

class ControllerLifecycleOwner<T>(lifecycleController: T) : LifecycleOwner where T : Controller, T : LifecycleOwner {

  private val lifecycleRegistry = LifecycleRegistry(lifecycleController)

  override fun getLifecycle(): Lifecycle = lifecycleRegistry

  init {
    lifecycleController.addLifecycleListener(object : LifecycleListener() {
      override fun postContextAvailable(controller: Controller, context: Context) {
        lifecycleRegistry.handleLifecycleEvent(Event.ON_CREATE)
      }

      override fun preCreateView(controller: Controller) {
        lifecycleRegistry.handleLifecycleEvent(Event.ON_START)
      }

      override fun preAttach(controller: Controller, view: View) {
        lifecycleRegistry.handleLifecycleEvent(Event.ON_RESUME)
      }

      override fun preDetach(controller: Controller, view: View) {
        lifecycleRegistry.handleLifecycleEvent(Event.ON_PAUSE)
      }

      override fun preDestroyView(controller: Controller, view: View) {
        lifecycleRegistry.handleLifecycleEvent(Event.ON_STOP)
      }

      override fun preDestroy(controller: Controller) {
        lifecycleRegistry.handleLifecycleEvent(Event.ON_DESTROY)
      }
    })
  }
}
