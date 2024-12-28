package de.paulwoitaschek.pref.android.internal.adapter

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import voice.pref.internal.adapter.EnumAdapter

@RunWith(AndroidJUnit4::class)
class EnumAdapterTest {

  @Test
  fun test() {
    AdapterTester(EnumAdapter(RPS::class.java)).apply {
      test(RPS.Rock)
      test(RPS.Paper)
      test(RPS.Scissors)
      test(RPS.Rock)
    }
  }

  private enum class RPS {
    Rock,
    Paper,
    Scissors,
  }
}
