package voice.pref.internal.adapter

import android.content.SharedPreferences
import androidx.core.content.edit
import voice.pref.internal.InternalPrefAdapter

internal object LongAdapter : InternalPrefAdapter<Long> {

  override fun get(
    key: String,
    prefs: SharedPreferences,
  ): Long {
    return prefs.getLong(key, 0)
  }

  override fun set(
    key: String,
    prefs: SharedPreferences,
    value: Long,
    commit: Boolean,
  ) {
    prefs.edit(commit = commit) { putLong(key, value) }
  }
}
