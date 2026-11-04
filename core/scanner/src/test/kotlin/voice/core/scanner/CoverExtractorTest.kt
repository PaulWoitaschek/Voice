package voice.core.scanner

import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import voice.core.scanner.matroska.MatroskaCoverExtractor

@RunWith(AndroidJUnit4::class)
class CoverExtractorTest {

  @get:Rule val temporaryFolder = TemporaryFolder()

  private val coverExtractor =
    CoverExtractor(
      context = ApplicationProvider.getApplicationContext(),
      matroskaCoverExtractor = MatroskaCoverExtractor(ApplicationProvider.getApplicationContext()),
    )

  @Test
  fun `a file that never opens is given up on`() = runTest {
    val file = temporaryFolder.newStalledFile("stalled.mp3")

    assertNull(coverExtractor.extractCover(file.toUri(), temporaryFolder.newFile("cover.jpg")))
  }
}
