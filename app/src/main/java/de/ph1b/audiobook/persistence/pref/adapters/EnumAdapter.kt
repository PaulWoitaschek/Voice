package de.ph1b.audiobook.persistence.pref.adapters

import android.content.SharedPreferences
import androidx.core.content.edit
import de.ph1b.audiobook.persistence.pref.PrefAdapter

class EnumAdapter<E : Enum<E>>(private val clazz: Class<E>) : PrefAdapter<E> {

  override fun get(key: String, prefs: SharedPreferences): E {
    val stringValue = prefs.getString(key, "")!!
    return java.lang.Enum.valueOf(clazz, stringValue)
  }

  override fun set(key: String, prefs: SharedPreferences, value: E) {
    prefs.edit { putString(key, value.name) }
  }
}
