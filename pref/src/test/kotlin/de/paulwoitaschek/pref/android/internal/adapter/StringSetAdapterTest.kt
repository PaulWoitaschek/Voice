package de.paulwoitaschek.pref.android.internal.adapter

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import voice.pref.internal.adapter.StringSetAdapter

@RunWith(AndroidJUnit4::class)
class StringSetAdapterTest {

  @Test
  fun test() {
    AdapterTester(StringSetAdapter).apply {
      test(emptySet())
      test(setOf("Hello"))
      test(setOf("Apple", "Candy"))
      test(emptySet())
    }
  }
}
