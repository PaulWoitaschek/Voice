package voice.app.misc

import androidx.annotation.RawRes
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import voice.app.scanner.FFProbeAnalyze
import voice.app.scanner.MediaAnalyzer
import voice.app.test.R
import java.io.File

@RunWith(AndroidJUnit4::class)
class MediaAnalyzerInstrumentationTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  private val mediaAnalyzer = MediaAnalyzer(FFProbeAnalyze(ApplicationProvider.getApplicationContext()))

  @Test(timeout = 1000)
  fun defectFile_noDuration() {
    val duration = durationOfResource(R.raw.defect)
    duration.shouldBeNull()
  }

  @Test
  fun intactFile_correctDuration() {
    val duration = durationOfResource(R.raw.intact)
    duration shouldBe 119040
  }

  @Test(timeout = 1000)
  fun subsequentCalls() {
    durationOfResource(R.raw.intact) shouldBe 119040
    durationOfResource(R.raw.defect).shouldBeNull()
    durationOfResource(R.raw.intact2) shouldBe 119040
  }

  private fun durationOfResource(@RawRes resource: Int): Long? {
    val file = resourceToTemporaryFile(resource)
    return runBlocking {
      mediaAnalyzer.analyze(DocumentFile.fromFile(file))?.duration
    }
  }

  private fun resourceToTemporaryFile(@RawRes resource: Int): File {
    val file = temporaryFolder.newFile()
    val context = InstrumentationRegistry.getInstrumentation().context
    context.resources.openRawResource(resource).use { input ->
      file.outputStream().use { output ->
        input.copyTo(output)
      }
    }

    require(file.length() > 0)
    return file
  }
}
