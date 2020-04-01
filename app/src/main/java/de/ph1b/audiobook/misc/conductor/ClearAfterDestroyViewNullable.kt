package de.ph1b.audiobook.misc.conductor

import com.bluelinelabs.conductor.Controller
import timber.log.Timber
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A property that clears the reference upon postDestroyView
 */
class ClearAfterDestroyViewNullable<T>(controller: Controller) : ReadWriteProperty<Controller, T?> {

  init {
    controller.addLifecycleListener(
      object : Controller.LifecycleListener {
        override fun postDestroyView(controller: Controller) {
          if (controller.isDestroyed || controller.isBeingDestroyed) {
            Timber.d("We are in teardown. Defer releasing the reference.")
          } else value = null
        }

        override fun postDestroy(controller: Controller) {
          value = null
        }
      }
    )
  }

  private var value: T? = null

  override fun getValue(thisRef: Controller, property: KProperty<*>): T? = value

  override fun setValue(thisRef: Controller, property: KProperty<*>, value: T?) {
    this.value = value
  }
}

fun <T> Controller.clearAfterDestroyViewNullable(): ReadWriteProperty<Controller, T?> =
  ClearAfterDestroyViewNullable(
    this
  )
