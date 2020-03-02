package de.ph1b.audiobook

import de.ph1b.audiobook.prefs.Pref
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlin.reflect.KProperty

class MemoryPref<T : Any>(default: T) : Pref<T>() {

  private val channel = ConflatedBroadcastChannel(default)

  override val flow: Flow<T>
    get() = channel.asFlow()

  override fun getValue(thisRef: Any, property: KProperty<*>): T = channel.value

  override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
    channel.offer(value)
  }
}
