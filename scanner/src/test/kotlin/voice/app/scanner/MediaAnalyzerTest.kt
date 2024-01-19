package voice.app.scanner

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import voice.documentfile.FileBasedDocumentFile
import java.io.File

internal class MediaAnalyzerTest {

  private val ffprobe = mockk<FFProbeAnalyze>()
  private val analyzer = MediaAnalyzer(ffprobe)

  @Test
  fun chapterNameUsed() = runTest {
    val file = FileBasedDocumentFile(File("mybook.mp3"))
    coEvery {
      ffprobe.analyze(any())
    } returns MetaDataScanResult(
      streams = listOf(MetaDataStream()),
      format = MetaDataFormat(
        tags = mapOf("title" to "MyTitle"),
        duration = 123.45,
      ),
    )
    analyzer.analyze(file)!!.title shouldBe "MyTitle"
  }

  @Test
  fun chapterFallbackDerivedFromFileName() = runTest {
    val file = FileBasedDocumentFile(File("mybook.mp3"))
    coEvery {
      ffprobe.analyze(any())
    } returns MetaDataScanResult(
      streams = listOf(MetaDataStream()),
      format = MetaDataFormat(
        duration = 123.45,
      ),
    )
    analyzer.analyze(file)!!.fileName shouldBe "mybook"
  }
}
