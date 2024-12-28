package de.paulwoitaschek.flowpref.android

import android.content.SharedPreferences
import de.paulwoitaschek.flowpref.Pref
import de.paulwoitaschek.flowpref.android.internal.AndroidPref
import de.paulwoitaschek.flowpref.android.internal.DelegatingPrefAdapter
import de.paulwoitaschek.flowpref.android.internal.InternalPrefAdapter
import java.util.concurrent.CopyOnWriteArrayList

class AndroidPreferences(private val sharedPrefs: SharedPreferences) {

  private val registered = CopyOnWriteArrayList<AndroidPref<*>>()

  @Suppress("unused")
  fun <T> create(
    key: String,
    default: T,
    adapter: PrefAdapter<T>,
  ): Pref<T> {
    return create(key, default, DelegatingPrefAdapter(adapter))
  }

  internal fun <T> create(
    key: String,
    default: T,
    adapter: InternalPrefAdapter<T>,
  ): Pref<T> {
    return AndroidPref(
      sharedPrefs,
      adapter,
      key,
      default,
    ).also {
      registered += it
    }
  }

  fun clear(commit: Boolean = false) {
    registered.forEach { it.delete(commit = commit) }
  }
}
