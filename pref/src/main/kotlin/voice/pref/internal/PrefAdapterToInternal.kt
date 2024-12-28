package voice.pref.internal

import android.content.SharedPreferences
import androidx.core.content.edit
import voice.pref.PrefAdapter

internal class DelegatingPrefAdapter<T>(private val delegate: PrefAdapter<T>) : InternalPrefAdapter<T> {

  override fun get(
    key: String,
    prefs: SharedPreferences,
  ): T {
    val stringValue = prefs.getString(key, null)!!
    return delegate.fromString(stringValue)
  }

  override fun set(
    key: String,
    prefs: SharedPreferences,
    value: T,
    commit: Boolean,
  ) {
    val stringValue = delegate.toString(value)
    prefs.edit(commit = commit) {
      putString(key, stringValue)
    }
  }
}
