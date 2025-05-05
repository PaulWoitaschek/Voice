package voice.app.scanner

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.longs.shouldBeWithinPercentageOf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import voice.data.MarkData
import voice.documentfile.FileBasedDocumentFile
import voice.logging.core.LogWriter
import voice.logging.core.Logger
import java.io.File
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
internal class MediaAnalyzerTest {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  private val analyzer = MediaAnalyzer(ApplicationProvider.getApplicationContext())

  @Test
  fun mp3() = test(
    fileName = "test.mp3",
    durationAssert = {
      it.shouldBeWithinPercentageOf(30.seconds.inWholeMilliseconds, 0.2)
    },
  )

  private fun test(
    fileName: String,
    durationAssert: (Long) -> Unit = {
      it shouldBe 30.seconds.inWholeMilliseconds
    },
  ) {
    runTest {
      Logger.install(
        object : LogWriter {
          override fun log(
            severity: Logger.Severity,
            message: String,
            throwable: Throwable?,
          ) {
            println("$severity: $message, $throwable")
          }
        },
      )
      val testFile = File(javaClass.classLoader!!.getResource(fileName)!!.file)
      val documentFile = FileBasedDocumentFile(testFile)
      val metadata = analyzer.analyze(documentFile)

      assertSoftly {
        metadata.shouldNotBeNull()
        durationAssert(metadata.duration)
        metadata.chapters.shouldBe(
          listOf(
            MarkData(
              startMs = 0L,
              name = "Introduction",
            ),
            MarkData(
              startMs = 10000,
              name = "Chapter 1",
            ),
            MarkData(
              startMs = 20000,
              name = "Chapter 2",
            ),
          ),
        )
        metadata.title shouldBe "Test Audiobook"
        metadata.artist shouldBe "Jane Doe"
        metadata.fileName shouldBe "test"
        metadata.album shouldBe "Sample Audio"
      }
    }
  }
}
