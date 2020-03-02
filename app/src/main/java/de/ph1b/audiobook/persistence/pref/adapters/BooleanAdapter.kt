package de.ph1b.audiobook.persistence.pref.adapters

import android.content.SharedPreferences
import androidx.core.content.edit
import de.ph1b.audiobook.persistence.pref.PrefAdapter

object BooleanAdapter : PrefAdapter<Boolean> {

  override fun get(key: String, prefs: SharedPreferences): Boolean {
    return prefs.getBoolean(key, false)
  }

  override fun set(key: String, prefs: SharedPreferences, value: Boolean) {
    prefs.edit { putBoolean(key, value) }
  }
}
