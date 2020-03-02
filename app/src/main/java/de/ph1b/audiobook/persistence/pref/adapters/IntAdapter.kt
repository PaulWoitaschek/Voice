package de.ph1b.audiobook.persistence.pref.adapters

import android.content.SharedPreferences
import androidx.core.content.edit
import de.ph1b.audiobook.persistence.pref.PrefAdapter

object IntAdapter : PrefAdapter<Int> {

  override fun get(key: String, prefs: SharedPreferences): Int {
    return prefs.getInt(key, 0)
  }

  override fun set(key: String, prefs: SharedPreferences, value: Int) {
    prefs.edit { putInt(key, value) }
  }
}
