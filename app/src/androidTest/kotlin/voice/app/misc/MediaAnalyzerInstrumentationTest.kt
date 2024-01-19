package voice.app.misc

import androidx.annotation.RawRes
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
import voice.data.MarkData
import voice.documentfile.FileBasedDocumentFile
import java.io.File

@RunWith(AndroidJUnit4::class)
class MediaAnalyzerInstrumentationTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  private val mediaAnalyzer = MediaAnalyzer(FFProbeAnalyze(ApplicationProvider.getApplicationContext()))

  @Test
  fun defectFile() {
    analyze(R.raw.defect).shouldBeNull()
  }

  @Test
  fun intactFile() {
    analyze(R.raw.intact)!!.copy(fileName = "") shouldBe MediaAnalyzer.Metadata(
      duration = 119040,
      artist = "Auphonic",
      album = "Auphonic Examples",
      fileName = "",
      title = "Auphonic Chapter Marks Demo",
      chapters = listOf(
        MarkData(0, "Intro"),
        MarkData(15_000, "Creating a new production"),
        MarkData(22000, "Sound analysis"),
        MarkData(34000, "Adaptive leveler"),
        MarkData(45000, "Global loudness normalization"),
        MarkData(60000, "Audio restoration algorithms"),
        MarkData(76000, "Output file formats"),
        MarkData(94000, "External services"),
        MarkData(111500, "Get a free account!"),
      ),
    )
  }

  private fun analyze(@RawRes resource: Int): MediaAnalyzer.Metadata? {
    val file = resourceToTemporaryFile(resource)
    return runBlocking {
      mediaAnalyzer.analyze(FileBasedDocumentFile(file))
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
