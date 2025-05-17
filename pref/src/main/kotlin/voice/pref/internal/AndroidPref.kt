package voice.pref.internal

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import voice.pref.Pref
import kotlin.reflect.KProperty

internal class AndroidPref<T>(
  private val prefs: SharedPreferences,
  private val adapter: InternalPrefAdapter<T>,
  val key: String,
  private val default: T,
) : Pref<T>() {

  private val channel = MutableStateFlow(valueFromPrefOrDefault())

  private fun valueFromPrefOrDefault(): T {
    return if (prefs.contains(key)) {
      adapter.get(key, prefs)
    } else {
      default
    }
  }

  override val data: Flow<T>
    get() = channel

  override fun getValue(
    thisRef: Any,
    property: KProperty<*>,
  ): T = channel.value

  override fun setValue(
    thisRef: Any,
    property: KProperty<*>,
    value: T,
  ) {
    set(value, commit = false)
  }

  override suspend fun updateData(transform: suspend (t: T) -> T): T {
    val value = adapter.get(key, prefs)
    val transformed = transform(value)
    adapter.set(key, prefs, transformed, commit = true)
    channel.value = transformed
    return transformed
  }

  private fun set(
    value: T,
    commit: Boolean,
  ) {
    adapter.set(key, prefs, value, commit = commit)
    channel.value = value
  }
}
