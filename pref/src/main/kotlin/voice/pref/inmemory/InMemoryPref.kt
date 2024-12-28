package voice.pref.inmemory

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import voice.pref.Pref
import kotlin.reflect.KProperty

class InMemoryPref<T>(private val default: T) : Pref<T>() {

  private val channel = MutableStateFlow(default)

  override fun setAndCommit(value: T) {
    channel.tryEmit(value)
  }

  override val flow: Flow<T> = channel

  override fun delete(commit: Boolean) {
    channel.tryEmit(default)
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
