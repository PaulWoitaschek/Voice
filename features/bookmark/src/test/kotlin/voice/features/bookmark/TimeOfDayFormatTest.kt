package voice.features.bookmark

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import java.time.LocalTime
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class TimeOfDayFormatTest {

  private val evening = LocalTime.of(22, 58)

  @Test
  fun `the 24-hour setting drops AM and PM`() {
    assertEquals(expected = "22:58", actual = TimeOfDayFormat(Locale.US, is24HourFormat = true).format(evening))
  }

  @Test
  fun `the 12-hour setting adds AM and PM, also where the language uses 24 hours`() {
    assertEquals(expected = "10:58 PM", actual = TimeOfDayFormat(Locale.US, is24HourFormat = false).format(evening).plainSpaces())
    assertTrue(TimeOfDayFormat(Locale.GERMANY, is24HourFormat = false).format(evening).startsWith("10:58"))
  }

  @Test
  fun `languages that name the time of the day work too`() {
    // traditional Chinese says "at night" instead of PM, a pattern java.time can't read on older Android versions
    assertTrue("10:58" in TimeOfDayFormat(Locale.TAIWAN, is24HourFormat = false).format(evening))
  }
}

/** ICU puts a narrow no-break space before AM and PM. */
private fun String.plainSpaces(): String = replace(Regex("""\p{Zs}"""), " ")
