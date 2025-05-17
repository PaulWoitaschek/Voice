package voice.pref.inmemory

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import voice.pref.Pref
import kotlin.reflect.KProperty

class InMemoryPref<T>(private val default: T) : Pref<T>() {

  private val channel = MutableStateFlow(default)

  override val data: Flow<T>
    get() = channel

  override suspend fun updateData(transform: suspend (t: T) -> T): T {
    val value = channel.first()
    val newValue = transform(value)
    channel.value = newValue
    return newValue
  }

  override fun getValue(
    thisRef: Any,
    property: KProperty<*>,
  ): T {
    return channel.value
  }

  override fun setValue(
    thisRef: Any,
    property: KProperty<*>,
    value: T,
  ) {
    channel.tryEmit(value)
  }
}
