package de.paulwoitaschek.flowpref.android.internal.adapter

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntAdapterTest {

  @Test
  fun test() {
    AdapterTester(IntAdapter).apply {
      test(0)
      test(1)
      test(2)
      test(Int.MAX_VALUE)
      test(Int.MIN_VALUE)
    }
  }
}
