package voice.core.logging.crashlytics

import java.io.FileNotFoundException
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class CrashlyticsMessageTest {

  @Test
  fun `a message is kept as it is`() {
    assertEquals(
      expected = "Scan took 120 ms",
      actual = crashlyticsMessage("Scan took 120 ms", throwable = null),
    )
  }

  @Test
  fun `exceptions show only their types`() {
    val throwable = IOException(
      "content://com.android.externalstorage.documents/tree/primary%3AAudiobooks",
      FileNotFoundException("/storage/emulated/0/Audiobooks/My Book/01.mp3"),
    )
    assertEquals(
      expected = "Could not open the file: java.io.IOException caused by java.io.FileNotFoundException",
      actual = crashlyticsMessage("Could not open the file", throwable),
    )
  }

  @Test
  fun `a non-fatal logs only its type`() {
    assertEquals(
      expected = "java.lang.IllegalStateException",
      actual = crashlyticsMessage(message = null, IllegalStateException("Skipping a book with 30000 chapters")),
    )
  }
}
