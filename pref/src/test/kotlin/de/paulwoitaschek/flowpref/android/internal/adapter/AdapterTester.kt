package de.paulwoitaschek.flowpref.android.internal.adapter

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import de.paulwoitaschek.flowpref.android.internal.InternalPrefAdapter
import io.kotest.matchers.shouldBe

internal class AdapterTester<T>(private val adapter: InternalPrefAdapter<T>) {

  private val key = System.nanoTime().toString()

  private val prefs: SharedPreferences = ApplicationProvider.getApplicationContext<Context>()
    .getSharedPreferences(key, Context.MODE_PRIVATE)

  fun test(value: T) {
    adapter.set(key, prefs, value)
    adapter.get(key, prefs) shouldBe value
  }
}
