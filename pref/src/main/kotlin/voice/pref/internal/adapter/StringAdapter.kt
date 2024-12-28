package voice.pref.internal.adapter

import android.content.SharedPreferences
import androidx.core.content.edit
import voice.pref.internal.InternalPrefAdapter

internal object StringAdapter : InternalPrefAdapter<String> {

  override fun get(
    key: String,
    prefs: SharedPreferences,
  ): String {
    return prefs.getString(key, null)!!
  }

  override fun set(
    key: String,
    prefs: SharedPreferences,
    value: String,
    commit: Boolean,
  ) {
    prefs.edit(commit = commit) {
      putString(key, value)
    }
  }
}
