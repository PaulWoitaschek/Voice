package voice.core.scanner

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveElementAt
import io.kotest.matchers.longs.shouldBeWithinPercentageOf
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import voice.core.data.MarkData
import voice.core.documentfile.FileBasedDocumentFile
import voice.core.logging.core.LogWriter
import voice.core.logging.core.Logger
import voice.core.scanner.MediaAnalyzer
import voice.core.scanner.Metadata
import voice.core.scanner.matroska.MatroskaMetaDataExtractor
import voice.core.scanner.mp4.ChapterTrackProcessor
import voice.core.scanner.mp4.Mp4BoxParser
import voice.core.scanner.mp4.Mp4ChapterExtractor
import voice.core.scanner.mp4.visitor.ChapVisitor
import voice.core.scanner.mp4.visitor.ChplVisitor
import voice.core.scanner.mp4.visitor.MdhdVisitor
import voice.core.scanner.mp4.visitor.StcoVisitor
import voice.core.scanner.mp4.visitor.StscVisitor
import voice.core.scanner.mp4.visitor.SttsVisitor
import java.io.File
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
internal class MediaAnalyzerTest {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  private val analyzer = MediaAnalyzer(
    context = ApplicationProvider.getApplicationContext(),
    mp4ChapterExtractor = Mp4ChapterExtractor(
      context = ApplicationProvider.getApplicationContext(),
      boxParser = Mp4BoxParser(
        stscVisitor = StscVisitor(),
        mdhdVisitor = MdhdVisitor(),
        sttsVisitor = SttsVisitor(),
        stcoVisitor = StcoVisitor(),
        chplVisitor = ChplVisitor(),
        chapVisitor = ChapVisitor(),
      ),
      chapterTrackProcessor = ChapterTrackProcessor(),
    ),
    matroskaExtractorFactory = MatroskaMetaDataExtractor.Factory(
      context = ApplicationProvider.getApplicationContext(),
    ),
  )
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
      metadata.genre shouldBe "Podcast"
      metadata.narrator shouldBe "Auphonic Narrator"
      metadata.series shouldBe "Auphonic Movement"
      metadata.part shouldBe "2.1"
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
  fun mka_simple_chapters() {
    val metadata = parse("mka_simple_chapters.mka")
    assertSoftly {
      metadata.shouldNotBeNull()
      metadata.title shouldBe "Your Album Title"
      metadata.artist shouldBe "Your Artist Name"
      metadata.album shouldBe "Your Album Name"
      metadata.chapters.shouldContainExactly(
        MarkData(startMs = 0L, name = "Intro"),
        MarkData(startMs = 150000L, name = "Baby prepares to rock"),
        MarkData(startMs = 162300L, name = "Baby rocks the house"),
      )
    }
  }

  @Test
  fun mka_nested_chapters() {
    val metadata = parse("mka_nested_chapters.mka")
    assertSoftly {
      metadata.shouldNotBeNull()
      metadata.title shouldBe "Your Album Title"
      metadata.artist shouldBe "Your Artist Name"
      metadata.album shouldBe "Your Album Name"
      println(metadata.chapters)
      metadata.chapters.shouldContainExactly(
        MarkData(startMs = 0L, name = "Introduction"),
        MarkData(startMs = 10.minutes.inWholeMilliseconds, name = "Main Content"),
        MarkData(startMs = 20.minutes.inWholeMilliseconds, name = "Conclusion"),
      )
    }
  }

  @Test
  fun chapterTrackId() {
    val chapters = parse("chapter_track_id.m4b")
      .shouldNotBeNull()
      .chapters
    chapters.shouldHaveElementAt(0, MarkData(0, "Opening Credits"))
    chapters.shouldHaveElementAt(107, MarkData(103121056, "Closing Credits"))
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
      metadata.chapters shouldContainExactly auphonicChapters.filter {
        // for some reason only this one is missing in the test files
        it.name != "Creating a new production"
      }
    }
  }

  @Test
  fun `m4a with chpl chapters`() {
    val metadata = parse("chpl.m4a")

    assertSoftly {
      metadata.shouldNotBeNull()
      metadata.chapters.shouldContainExactly(
        MarkData(startMs = 0L, name = "Introduction"),
        MarkData(startMs = 10000L, name = "Chapter 1"),
        MarkData(startMs = 20000L, name = "Chapter 2"),
      )
    }
  }

  companion object {

    @BeforeClass
    @JvmStatic
    fun setup() {
      Logger.install(
        object : LogWriter {
          override fun log(
            severity: Logger.Severity,
            message: String,
            throwable: Throwable?,
          ) {
            println(
              buildString {
                append("${severity.name}: ")
                append(message)
                if (throwable != null) {
                  append(", $throwable")
                }
              },
            )
          }
        },
      )
    }
  }
}
