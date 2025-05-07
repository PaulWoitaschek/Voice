package voice.app.scanner

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.longs.shouldBeWithinPercentageOf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
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

@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
internal class MediaAnalyzerTest {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  private val analyzer = MediaAnalyzer(ApplicationProvider.getApplicationContext())
  private val auphonicChapters = listOf(
    MarkData(startMs = 0L, name = "Intro"),
    MarkData(startMs = 15000L, name = "Creating a new production"),
    MarkData(startMs = 22000L, name = "Sound analysis"),
    MarkData(startMs = 34000L, name = "Adaptive leveler"),
    MarkData(startMs = 45000L, name = "Global loudness normalization"),
    MarkData(startMs = 60000L, name = "Audio restoration algorithms"),
    MarkData(startMs = 76000L, name = "Output file formats"),
    MarkData(startMs = 94000L, name = "External services"),
    MarkData(startMs = 111500L, name = "Get a free account!"),
  )

  private fun parse(filename: String): Metadata? {
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
    val testFile = File(javaClass.classLoader!!.getResource(filename)!!.file)
    val documentFile = FileBasedDocumentFile(testFile)
    return runBlocking { analyzer.analyze(documentFile) }
  }

  @Test
  fun mp3() {
    val metadata = parse("auphonic_chapters_demo.mp3")

    assertSoftly {
      metadata.shouldNotBeNull()
      metadata.duration.shouldBeWithinPercentageOf(119040L, 0.2)
      metadata.fileName shouldBe "auphonic_chapters_demo"
      metadata.title shouldBe "Auphonic Chapter Marks Demo"
      metadata.artist shouldBe "Auphonic"
      metadata.album shouldBe "Auphonic Examples"
      metadata.chapters shouldContainExactly auphonicChapters
    }
  }

  @Test
  fun ogg() {
    val metadata = parse("auphonic_chapters_demo.ogg")

    assertSoftly {
      metadata.shouldNotBeNull()
      metadata.duration.shouldBeWithinPercentageOf(119040L, 0.2)
      metadata.fileName shouldBe "auphonic_chapters_demo"
      metadata.title shouldBe "Auphonic Chapter Marks Demo"
      metadata.artist shouldBe "Auphonic"
      metadata.album shouldBe "Auphonic Examples"
      metadata.chapters shouldContainExactly auphonicChapters
    }
  }

  @Test
  fun opus() {
    val metadata = parse("auphonic_chapters_demo.opus")

    assertSoftly {
      metadata.shouldNotBeNull()
      metadata.duration.shouldBeWithinPercentageOf(119040L, 0.2)
      metadata.fileName shouldBe "auphonic_chapters_demo"
      metadata.title shouldBe "Auphonic Chapter Marks Demo"
      metadata.artist shouldBe "Auphonic"
      metadata.album shouldBe "Auphonic Examples"
      metadata.chapters shouldContainExactly auphonicChapters
    }
  }

  @Test
  fun m4a() {
    val metadata = parse("auphonic_chapters_demo.m4a")

    assertSoftly {
      metadata.shouldNotBeNull()
      metadata.duration.shouldBeWithinPercentageOf(119040L, 0.2)
      metadata.fileName shouldBe "auphonic_chapters_demo"
      metadata.title shouldBe "Auphonic Chapter Marks Demo"
      metadata.artist shouldBe "Auphonic"
      metadata.album shouldBe "Auphonic Examples"
    }
  }
}
