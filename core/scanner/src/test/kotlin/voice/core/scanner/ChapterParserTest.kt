package voice.core.scanner

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import voice.core.data.repo.ChapterRepoImpl
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.FileBasedDocumentFile
import voice.core.documentfile.nameWithoutExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class ChapterParserTest {

  private val testFolder = TemporaryFolder()

  @Rule
  fun testFolder() = testFolder

  @Before
  fun setUp() {
    testFolder.create()
  }

  @Test
  fun parserSorts() = runTest {
    val audiobook = testFolder.newFolder("audiobook")
    testFolder.newFile("audiobook/Chapter 1.mp3")
    testFolder.newFile("audiobook/Chapter 2.mp3")
    testFolder.newFile("audiobook/Chapter 20.mp3")
    testFolder.newFile("audiobook/Chapter 3.mp3")
    testFolder.newFile("audiobook/Chapter 30.mp3")

    assertEquals(
      expected = listOf(
        "Chapter 1",
        "Chapter 2",
        "Chapter 3",
        "Chapter 20",
        "Chapter 30",
      ),
      actual = chapterParser().parse(FileBasedDocumentFile(audiobook))
        .chapters
        .map { it.name },
    )
  }

  @Test
  fun `chapters follow the file names when the uris only have ids`() = runTest {
    val book = cloudFolder(
      cloudFile("Chapter 2.mp3", documentId = 1),
      cloudFile("Chapter 10.mp3", documentId = 2),
      cloudFile("Chapter 1.mp3", documentId = 3),
    )

    assertEquals(
      expected = listOf("Chapter 1", "Chapter 2", "Chapter 10"),
      actual = chapterParser().parse(book).chapters.map { it.name },
    )
  }

  @Test
  fun `files in subfolders come before the files next to them`() = runTest {
    val audiobook = testFolder.newFolder("audiobook")
    testFolder.newFolder("audiobook", "CD 1")
    testFolder.newFolder("audiobook", "CD 10")
    testFolder.newFolder("audiobook", "CD 2")
    testFolder.newFile("audiobook/CD 1/a.mp3")
    testFolder.newFile("audiobook/CD 10/c.mp3")
    testFolder.newFile("audiobook/CD 2/b.mp3")
    testFolder.newFile("audiobook/intro.mp3")

    assertEquals(
      expected = listOf("a", "b", "c", "intro"),
      actual = chapterParser().parse(FileBasedDocumentFile(audiobook)).chapters.map { it.name },
    )
  }

  @Test
  fun `the first chapter keeps its metadata when later files are analyzed first`() = runTest {
    val audiobook = testFolder.newFolder("audiobook")
    testFolder.newFile("audiobook/Chapter 1.mp3")
    testFolder.newFile("audiobook/Chapter 2.mp3")
    testFolder.newFile("audiobook/Chapter 3.mp3")

    val result = chapterParser(analysisTime = { name -> if (name == "Chapter 1") 3.seconds else 1.seconds })
      .parse(FileBasedDocumentFile(audiobook))

    assertEquals(expected = listOf("Chapter 1", "Chapter 2", "Chapter 3"), actual = result.chapters.map { it.name })
    assertEquals(expected = "Chapter 1", actual = result.firstChapterMetadata?.fileName)
  }

  private fun cloudFolder(vararg files: CachedDocumentFile): CachedDocumentFile = mockk {
    every { name } returns "Book"
    every { isDirectory } returns true
    every { isFile } returns false
    every { children } returns files.toList()
  }

  private fun cloudFile(
    fileName: String,
    documentId: Int,
  ): CachedDocumentFile = mockk {
    every { name } returns fileName
    every { uri } returns "content://org.nextcloud.documents/document/$documentId".toUri()
    every { isDirectory } returns false
    every { isFile } returns true
    every { length } returns 0
    every { lastModified } returns 0
  }

  private fun chapterParser(analysisTime: (String) -> Duration = { Duration.ZERO }) = ChapterParser(
    chapterRepo = ChapterRepoImpl(
      mockk {
        coEvery {
          chapter(any())
        } returns null
        coEvery {
          insert(any())
        } just Runs
      },
    ),
    mediaAnalyzer = mockk {
      coEvery {
        analyze(any())
      } coAnswers {
        val file = firstArg<CachedDocumentFile>()
        delay(analysisTime(file.nameWithoutExtension()))
        Metadata(
          duration = 1000,
          fileName = file.nameWithoutExtension(),
          artist = null,
          album = null,
          chapters = emptyList(),
          title = null,
          genre = null,
          narrator = null,
          series = null,
          part = null,
        )
      }
    },
  )
}
