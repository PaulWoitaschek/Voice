package de.ph1b.audiobook.misc.argumentDelegate


import android.os.Bundle
import com.bluelinelabs.conductor.Controller
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Argument delegate that handles writing the property to a bundle using the property name
 *
 * @author Paul Woitaschek
 */
abstract class ArgumentDelegate<T : Any> : ReadWriteProperty<Controller, T> {

  abstract fun write(args: Bundle, key: String, value: T)
  abstract fun read(args: Bundle, key: String): T

  private var value: T? = null

  override final operator fun setValue(thisRef: Controller, property: KProperty<*>, value: T) {
    val args = thisRef.args
    val key = property.name
    write(args, key, value)
  }

  override final operator fun getValue(thisRef: Controller, property: KProperty<*>): T {
    if (value == null) {
      val args = thisRef.args
      val key = property.name
      value = read(args, key)
    }
    return value!!
  }
}
