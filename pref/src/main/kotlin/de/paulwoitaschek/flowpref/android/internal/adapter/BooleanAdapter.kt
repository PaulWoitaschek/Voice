package de.paulwoitaschek.flowpref.android.internal.adapter

import android.content.SharedPreferences
import androidx.core.content.edit
import de.paulwoitaschek.flowpref.android.internal.InternalPrefAdapter

internal object BooleanAdapter : InternalPrefAdapter<Boolean> {

  override fun get(
    key: String,
    prefs: SharedPreferences,
  ): Boolean {
    return prefs.getBoolean(key, false)
  }

  override fun set(
    key: String,
    prefs: SharedPreferences,
    value: Boolean,
    commit: Boolean,
  ) {
    prefs.edit(commit = commit) { putBoolean(key, value) }
  }
}
