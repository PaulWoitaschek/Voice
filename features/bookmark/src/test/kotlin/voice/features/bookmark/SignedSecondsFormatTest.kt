package voice.features.bookmark

import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SignedSecondsFormatTest {

  @Test
  fun `seconds show their sign`() {
    val format = signedSecondsFormat(Locale.US)
    assertEquals(expected = "+15s", actual = format.format(Measure(15, MeasureUnit.SECOND)))
    assertEquals(expected = "-15s", actual = format.format(Measure(-15, MeasureUnit.SECOND)))
  }

  @Test
  fun `seconds are in the user's language`() {
    val later = signedSecondsFormat(Locale.forLanguageTag("ar-EG")).format(Measure(15, MeasureUnit.SECOND))
    // Arabic digits and unit
    assertTrue("١٥" in later)
    assertTrue("s" !in later)
  }
}
