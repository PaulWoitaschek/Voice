package de.paulwoitaschek.pref.android.internal.adapter

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import voice.pref.internal.adapter.StringAdapter

@RunWith(AndroidJUnit4::class)
class StringAdapterTest {

  @Test
  fun test() {
    AdapterTester(StringAdapter).apply {
      test("")
      test("hello")
      test("bye")
      test("")
    }
  }
}
