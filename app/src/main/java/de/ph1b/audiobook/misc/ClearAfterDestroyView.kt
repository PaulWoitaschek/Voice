package de.ph1b.audiobook.misc

import com.bluelinelabs.conductor.Controller
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A property that clears the reference upon postDestroyView
 */
class ClearAfterDestroyView<T : Any>(controller: Controller) : ReadWriteProperty<Controller, T> {

  init {
    controller.addLifecycleListener(object : Controller.LifecycleListener() {
      override fun postDestroyView(controller: Controller) {
        value = null
      }
    })
  }

  private var value: T? = null

  override fun getValue(thisRef: Controller, property: KProperty<*>): T =
      value ?: throw UninitializedPropertyAccessException("Property ${property.name} is not initialized.")

  override fun setValue(thisRef: Controller, property: KProperty<*>, value: T) {
    this.value = value
  }
}

fun <T : Any> Controller.clearAfterDestroyView(): ReadWriteProperty<Controller, T> = ClearAfterDestroyView(this)
