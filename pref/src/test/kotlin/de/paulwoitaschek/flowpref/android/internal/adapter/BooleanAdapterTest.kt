package de.paulwoitaschek.flowpref.android.internal.adapter

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BooleanAdapterTest {

  @Test
  fun test() {
    AdapterTester(BooleanAdapter).apply {
      test(true)
      test(false)
      test(true)
    }
  }
}
