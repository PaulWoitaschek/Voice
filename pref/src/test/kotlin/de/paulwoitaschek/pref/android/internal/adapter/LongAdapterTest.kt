package de.paulwoitaschek.pref.android.internal.adapter

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import voice.pref.internal.adapter.LongAdapter

@RunWith(AndroidJUnit4::class)
class LongAdapterTest {

  @Test
  fun test() {
    AdapterTester(LongAdapter).apply {
      test(0)
      test(1)
      test(2)
      test(Long.MAX_VALUE)
      test(Long.MIN_VALUE)
    }
  }
}
