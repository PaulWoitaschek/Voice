package voice.core.scanner

import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.CachedDocumentFileFactory
import voice.core.documentfile.FileBasedDocumentFactory
import voice.core.documentfile.FileBasedDocumentFile
import java.io.File
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class CoverScannerTest {

  @get:Rule
  val testFolder = TemporaryFolder()

  private val savedCover by lazy { File(testFolder.root, "saved.png") }
  private val coverSaver = mockk<CoverSaver>(relaxUnitFun = true) {
    coEvery { newBookCoverFile() } answers { savedCover }
  }
  private val coverExtractor = mockk<CoverExtractor> {
    coEvery { extractCover(any(), any()) } returns false
  }
  private val scanner = scanner(FileBasedDocumentFactory)

  @Test
  fun `an image in the book folder becomes the cover`() = runTest {
    val folder = testFolder.newFolder("Book")
    File(folder, "1.mp3").writeText("audio")
    File(folder, "Cover.JPG").writeText("cover")
    val book = book(folder)

    scanner.scan(listOf(book))

    coVerify { coverSaver.setCoverFromFiles(savedCover, book.id) }
    assertEquals(expected = "cover", actual = savedCover.readText())
  }

  @Test
  fun `a file that is no image does not become the cover`() = runTest {
    val folder = testFolder.newFolder("Book")
    File(folder, "1.mp3").writeText("audio")
    File(folder, "notes.txt").writeText("notes")

    scanner.scan(listOf(book(folder)))

    coVerify(exactly = 0) { coverSaver.setCoverFromFiles(any(), any()) }
  }

  @Test
  fun `an image named cover is preferred over other images`() = runTest {
    val folder = testFolder.newFolder("Book")
    File(folder, "1.mp3").writeText("audio")
    val back = File(folder, "back.jpg").apply { writeText("back") }
    val cover = File(folder, "Cover.png").apply { writeText("cover") }
    val listedBackFirst = mockk<CachedDocumentFile> {
      every { children } returns listOf(FileBasedDocumentFile(back), FileBasedDocumentFile(cover))
    }

    scanner(mockk { every { create(any()) } returns listedBackFirst }).scan(listOf(book(folder)))

    assertEquals(expected = "cover", actual = savedCover.readText())
  }

  private fun scanner(documentFileFactory: CachedDocumentFileFactory) = CoverScanner(
    context = ApplicationProvider.getApplicationContext(),
    coverSaver = coverSaver,
    coverExtractor = coverExtractor,
    documentFileFactory = documentFileFactory,
  )

  private fun book(folder: File): Book {
    val chapters = folder.listFiles()!!
      .filter { it.extension == "mp3" }
      .map { chapter(it) }
    val content = BookParser(contentRepo = mockk(), mediaAnalyzer = mockk(), fileFactory = mockk())
      .parse(chapters, BookId(folder.toUri()), analyzed = null, file = FileBasedDocumentFile(folder))
    return Book(content, chapters)
  }

  private fun chapter(file: File): Chapter = Chapter(
    id = ChapterId(file.toUri()),
    name = "Chapter",
    duration = 1000L,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
    fileSize = 0,
  )
}
