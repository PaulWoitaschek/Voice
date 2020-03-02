package de.ph1b.audiobook.persistence.pref

import android.content.SharedPreferences
import de.ph1b.audiobook.persistence.pref.adapters.BooleanAdapter
import de.ph1b.audiobook.persistence.pref.adapters.EnumAdapter
import de.ph1b.audiobook.persistence.pref.adapters.IntAdapter
import de.ph1b.audiobook.persistence.pref.adapters.StringSetAdapter
import de.ph1b.audiobook.persistence.pref.adapters.UUIDAdapter
import de.ph1b.audiobook.prefs.Pref
import java.util.UUID

class ReactivePrefs(private val sharedPrefs: SharedPreferences) {

  private val registered = mutableListOf<PersistentPref<*>>()

  // because the shared preferences use a week reference when registering, we need to keep a reference
  private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
    registered.forEach { pref ->
      if (pref.key == key) {
        pref.notifyChanged()
      }
    }
  }

  init {
    sharedPrefs.registerOnSharedPreferenceChangeListener(listener)
  }

  fun boolean(key: String, default: Boolean): Pref<Boolean> {
    return create(BooleanAdapter, key, default)
  }

  fun int(key: String, default: Int): Pref<Int> {
    return create(IntAdapter, key, default)
  }

  fun stringSet(key: String, default: Set<String>): Pref<Set<String>> {
    return create(StringSetAdapter, key, default)
  }

  fun uuid(key: String, default: UUID): Pref<UUID> {
    return create(UUIDAdapter, key, default)
  }

  fun <E : Enum<E>> enum(key: String, default: E, clazz: Class<E>): Pref<E> {
    return create(EnumAdapter(clazz), key, default)
  }

  inline fun <reified E : Enum<E>> enum(key: String, default: E): Pref<E> {
    return enum(key, default, E::class.java)
  }

  private fun <T> create(adapter: PrefAdapter<T>, key: String, default: T): Pref<T> {
    return PersistentPref(sharedPrefs, adapter, key, default).also {
      registered += it
    }
  }
}
