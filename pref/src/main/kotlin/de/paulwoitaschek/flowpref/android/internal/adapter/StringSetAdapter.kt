package de.paulwoitaschek.flowpref.android.internal.adapter

import android.content.SharedPreferences
import androidx.core.content.edit
import de.paulwoitaschek.flowpref.android.internal.InternalPrefAdapter

internal object StringSetAdapter : InternalPrefAdapter<Set<String>> {

  override fun get(
    key: String,
    prefs: SharedPreferences,
  ): Set<String> {
    return prefs.getStringSet(key, emptySet())!!
  }

  override fun set(
    key: String,
    prefs: SharedPreferences,
    value: Set<String>,
    commit: Boolean,
  ) {
    prefs.edit(commit = commit) { putStringSet(key, value) }
  }
}
