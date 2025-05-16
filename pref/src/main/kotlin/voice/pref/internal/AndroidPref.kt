package voice.pref.internal

import android.content.SharedPreferences
import androidx.core.content.edit
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

  override fun setAndCommit(value: T) {
    set(value, commit = true)
  }

  private fun set(
    value: T,
    commit: Boolean,
  ) {
    adapter.set(key, prefs, value, commit = commit)
    channel.value = value
  }

  override fun delete(commit: Boolean) {
    prefs.edit(commit = commit) { remove(key) }
    channel.value = default
  }
}
