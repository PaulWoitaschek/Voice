package de.ph1b.audiobook.persistence.pref

import android.content.SharedPreferences

interface PrefAdapter<T> {

  fun get(key: String, prefs: SharedPreferences): T

  fun set(key: String, prefs: SharedPreferences, value: T)
}
