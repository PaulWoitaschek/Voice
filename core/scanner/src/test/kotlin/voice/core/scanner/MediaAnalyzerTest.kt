package voice.core.scanner

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import voice.core.data.MarkData
import voice.core.documentfile.FileBasedDocumentFile
import voice.core.logging.api.LogWriter
import voice.core.logging.api.Logger
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
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
    val metadata = assertNotNull(parse("auphonic_chapters_demo.mp3"))

    assertWithinPercentage(119040L, metadata.duration, 0.2)
    assertEquals(expected = "auphonic_chapters_demo", actual = metadata.fileName)
    assertEquals(expected = "Auphonic Chapter Marks Demo", actual = metadata.title)
    assertEquals(expected = "Auphonic", actual = metadata.artist)
    assertEquals(expected = "Auphonic Examples", actual = metadata.album)
    assertEquals(expected = "Podcast", actual = metadata.genre)
    assertEquals(expected = "Auphonic Narrator", actual = metadata.narrator)
    assertEquals(expected = "Auphonic Movement", actual = metadata.series)
    assertEquals(expected = "2.1", actual = metadata.part)
    assertEquals(expected = auphonicChapters, actual = metadata.chapters)
  }

  @Test
  fun ogg() {
    val metadata = assertNotNull(parse("auphonic_chapters_demo.ogg"))

    assertWithinPercentage(119040L, metadata.duration, 0.2)
    assertEquals(expected = "auphonic_chapters_demo", actual = metadata.fileName)
    assertEquals(expected = "Auphonic Chapter Marks Demo", actual = metadata.title)
    assertEquals(expected = "Auphonic", actual = metadata.artist)
    assertEquals(expected = "Auphonic Examples", actual = metadata.album)
    assertEquals(expected = auphonicChapters, actual = metadata.chapters)
  }

  @Test
  fun mka_simple_chapters() {
    val metadata = assertNotNull(parse("mka_simple_chapters.mka"))

    assertEquals(expected = "Your Album Title", actual = metadata.title)
    assertEquals(expected = "Your Artist Name", actual = metadata.artist)
    assertEquals(expected = "Your Album Name", actual = metadata.album)
    assertEquals(
      expected = listOf(
        MarkData(startMs = 0L, name = "Intro"),
        MarkData(startMs = 150000L, name = "Baby prepares to rock"),
        MarkData(startMs = 162300L, name = "Baby rocks the house"),
      ),
      actual = metadata.chapters,
    )
  }

  @Test
  fun mka_nested_chapters() {
    val metadata = assertNotNull(parse("mka_nested_chapters.mka"))

    assertEquals(expected = "Your Album Title", actual = metadata.title)
    assertEquals(expected = "Your Artist Name", actual = metadata.artist)
    assertEquals(expected = "Your Album Name", actual = metadata.album)
    assertEquals(
      expected = listOf(
        MarkData(startMs = 0L, name = "Introduction"),
        MarkData(startMs = 10.minutes.inWholeMilliseconds, name = "Main Content"),
        MarkData(startMs = 20.minutes.inWholeMilliseconds, name = "Conclusion"),
      ),
      actual = metadata.chapters,
    )
  }

  @Test
  fun chapterTrackId() {
    val chapters = assertNotNull(parse("chapter_track_id.m4b")).chapters
    assertEquals(expected = MarkData(0, "Opening Credits"), actual = chapters[0])
    assertEquals(expected = MarkData(103121056, "Closing Credits"), actual = chapters[107])
  }

  @Test
  fun opus() {
    val metadata = assertNotNull(parse("auphonic_chapters_demo.opus"))

    assertWithinPercentage(119040L, metadata.duration, 0.2)
    assertEquals(expected = "auphonic_chapters_demo", actual = metadata.fileName)
    assertEquals(expected = "Auphonic Chapter Marks Demo", actual = metadata.title)
    assertEquals(expected = "Auphonic", actual = metadata.artist)
    assertEquals(expected = "Auphonic Examples", actual = metadata.album)
    assertEquals(expected = auphonicChapters, actual = metadata.chapters)
  }

  @Test
  fun m4a() {
    val metadata = assertNotNull(parse("auphonic_chapters_demo.m4a"))

    assertWithinPercentage(119040L, metadata.duration, 0.2)
    assertEquals(expected = "auphonic_chapters_demo", actual = metadata.fileName)
    assertEquals(expected = "Auphonic Chapter Marks Demo", actual = metadata.title)
    assertEquals(expected = "Auphonic", actual = metadata.artist)
    assertEquals(expected = "Auphonic Examples", actual = metadata.album)
    assertEquals(
      expected = auphonicChapters.filter {
        // for some reason only this one is missing in the test files
        it.name != "Creating a new production"
      },
      actual = metadata.chapters,
    )
  }

  @Test
  fun `m4a with chpl chapters`() {
    val metadata = assertNotNull(parse("chpl.m4a"))

    assertEquals(
      expected = listOf(
        MarkData(startMs = 0L, name = "Introduction"),
        MarkData(startMs = 10000L, name = "Chapter 1"),
        MarkData(startMs = 20000L, name = "Chapter 2"),
      ),
      actual = metadata.chapters,
    )
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

private fun assertWithinPercentage(
  expected: Long,
  actual: Long,
  percentage: Double,
) {
  val tolerance = expected * percentage / 100
  assertTrue(actual in (expected - tolerance).toLong()..(expected + tolerance).toLong())
}
