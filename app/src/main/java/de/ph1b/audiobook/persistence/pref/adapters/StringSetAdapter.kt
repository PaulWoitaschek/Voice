package de.ph1b.audiobook.persistence.pref.adapters

import android.content.SharedPreferences
import androidx.core.content.edit
import de.ph1b.audiobook.persistence.pref.PrefAdapter

object StringSetAdapter : PrefAdapter<Set<String>> {

  override fun get(key: String, prefs: SharedPreferences): Set<String> {
    return prefs.getStringSet(key, emptySet())!!
  }

  override fun set(key: String, prefs: SharedPreferences, value: Set<String>) {
    prefs.edit { putStringSet(key, value) }
  }
}
