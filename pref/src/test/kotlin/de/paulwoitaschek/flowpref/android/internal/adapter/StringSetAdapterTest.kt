package de.paulwoitaschek.flowpref.android.internal.adapter

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

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
