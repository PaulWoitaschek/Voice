package de.paulwoitaschek.pref.android.internal.adapter

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.kotest.matchers.shouldBe
import voice.pref.internal.InternalPrefAdapter

internal class AdapterTester<T>(private val adapter: InternalPrefAdapter<T>) {

  private val key = System.nanoTime().toString()

  private val prefs: SharedPreferences = ApplicationProvider.getApplicationContext<Context>()
    .getSharedPreferences(key, Context.MODE_PRIVATE)

  fun test(value: T) {
    adapter.set(key, prefs, value)
    adapter.get(key, prefs) shouldBe value
  }
}
