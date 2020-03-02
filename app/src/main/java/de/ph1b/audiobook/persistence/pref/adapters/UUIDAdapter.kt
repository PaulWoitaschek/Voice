package de.ph1b.audiobook.persistence.pref.adapters

import android.content.SharedPreferences
import androidx.core.content.edit
import de.ph1b.audiobook.persistence.pref.PrefAdapter
import java.util.UUID

object UUIDAdapter : PrefAdapter<UUID> {

  override fun get(key: String, prefs: SharedPreferences): UUID {
    val stringValue = prefs.getString(key, "")
    return UUID.fromString(stringValue)
  }

  override fun set(key: String, prefs: SharedPreferences, value: UUID) {
    prefs.edit { putString(key, value.toString()) }
  }
}
