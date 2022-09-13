package voice.common.conductor

import com.bluelinelabs.conductor.Controller
import voice.logging.core.Logger
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A property that clears the reference upon postDestroyView
 */
class ClearAfterDestroyView<T : Any>(controller: Controller) : ReadWriteProperty<Controller, T> {

  init {
    controller.addLifecycleListener(
      object : Controller.LifecycleListener() {
        override fun postDestroyView(controller: Controller) {
          if (controller.isDestroyed || controller.isBeingDestroyed) {
            Logger.d("We are in teardown. Defer releasing the reference.")
          } else {
            value = null
          }
        }

        override fun postDestroy(controller: Controller) {
          value = null
        }
      },
    )
  }

  private var value: T? = null

  override fun getValue(thisRef: Controller, property: KProperty<*>): T {
    return value
      ?: throw UninitializedPropertyAccessException(
        "Property ${property.name} is not initialized.",
      )
  }

  override fun setValue(thisRef: Controller, property: KProperty<*>, value: T) {
    this.value = value
  }
}

fun <T : Any> Controller.clearAfterDestroyView(): ReadWriteProperty<Controller, T> =
  ClearAfterDestroyView(
    this,
  )
