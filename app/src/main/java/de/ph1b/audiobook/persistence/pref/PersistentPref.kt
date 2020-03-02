package de.ph1b.audiobook.persistence.pref

import android.content.SharedPreferences
import de.ph1b.audiobook.prefs.Pref
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlin.reflect.KProperty

class PersistentPref<T>(
  private val prefs: SharedPreferences,
  private val adapter: PrefAdapter<T>,
  val key: String,
  private val default: T
) : Pref<T>() {

  private val channel = ConflatedBroadcastChannel<T>()

  init {
    notifyChanged()
  }

  fun notifyChanged() {
    val value = if (prefs.contains(key)) {
      adapter.get(key, prefs)
    } else {
      default
    }
    channel.offer(value)
  }

  override val flow: Flow<T> get() = channel.asFlow()

  override fun getValue(thisRef: Any, property: KProperty<*>): T = channel.value

  override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
    adapter.set(key, prefs, value)
  }
}
