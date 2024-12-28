package de.paulwoitaschek.flowpref.android.internal.adapter

import android.content.SharedPreferences
import androidx.core.content.edit
import de.paulwoitaschek.flowpref.android.internal.InternalPrefAdapter

internal object IntAdapter : InternalPrefAdapter<Int> {

  override fun get(
    key: String,
    prefs: SharedPreferences,
  ): Int {
    return prefs.getInt(key, 0)
  }

  override fun set(
    key: String,
    prefs: SharedPreferences,
    value: Int,
    commit: Boolean,
  ) {
    prefs.edit(commit = commit) { putInt(key, value) }
  }
}
