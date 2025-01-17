package voice.pref.internal

import android.content.SharedPreferences

internal interface InternalPrefAdapter<T> {

  fun get(
    key: String,
    prefs: SharedPreferences,
  ): T

  fun set(
    key: String,
    prefs: SharedPreferences,
    value: T,
    commit: Boolean = false,
  )
}
