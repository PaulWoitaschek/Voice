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
import kotlin.test.assertFalse

@RunWith(AndroidJUnit4::class)
class CoverScannerTest {

  @get:Rule
  val testFolder = TemporaryFolder()

  private val image by lazy { File(testFolder.root, "image") }
  private val savedImages = mutableListOf<String>()
  private val coverSaver = mockk<CoverSaver>(relaxUnitFun = true) {
    every { newTemporaryFile() } answers { image }
    coEvery { saveExtractedCover(any(), any()) } answers {
      savedImages += firstArg<File>().readText()
      true
    }
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

    scan(book)

    coVerify { val _ = coverSaver.saveExtractedCover(image, book.id) }
    assertEquals(expected = listOf("cover"), actual = savedImages)
    assertFalse(image.exists())
  }

  @Test
  fun `a file that is no image does not become the cover`() = runTest {
    val folder = testFolder.newFolder("Book")
    File(folder, "1.mp3").writeText("audio")
    File(folder, "notes.txt").writeText("notes")

    scan(book(folder))

    coVerify(exactly = 0) { val _ = coverSaver.saveExtractedCover(any(), any()) }
  }

  @Test
  fun `an image named cover is preferred over other images`() = runTest {
    val folder = testFolder.newFolder("Book")
    File(folder, "1.mp3").writeText("audio")
    val listedCoverLast = mockk<CachedDocumentFile> {
      every { children } returns listOf("back.jpg", "Folder.jpg", "Cover.png").map { name ->
        FileBasedDocumentFile(File(folder, name).apply { writeText(nameWithoutExtension) })
      }
    }

    scan(book(folder), scanner(mockk { every { create(any()) } returns listedCoverLast }))

    assertEquals(expected = listOf("Cover"), actual = savedImages)
  }

  @Test
  fun `an image that can't be decoded makes way for the next one`() = runTest {
    val folder = testFolder.newFolder("Book")
    File(folder, "1.mp3").writeText("audio")
    File(folder, "Cover.jpg").writeText("broken")
    File(folder, "back.jpg").writeText("back")
    coEvery { coverSaver.saveExtractedCover(any(), any()) } answers {
      savedImages += firstArg<File>().readText()
      savedImages.size > 1
    }

    scan(book(folder))

    assertEquals(expected = listOf("broken", "back"), actual = savedImages)
  }

  @Test
  fun `a picked cover that is gone makes way for the one in the files`() = runTest {
    val folder = testFolder.newFolder("Book")
    File(folder, "1.mp3").writeText("audio")
    File(folder, "cover.jpg").writeText("cover")
    val missing = File(testFolder.root, "picked.png")
    val book = book(folder).let { Book(it.content.copy(customCover = missing), it.chapters) }

    scan(book)

    coVerify { coverSaver.forgetCustomCover(book.id, missing) }
    assertEquals(expected = listOf("cover"), actual = savedImages)
  }

  @Test
  fun `only the first file is asked for an embedded cover`() = runTest {
    val folder = testFolder.newFolder("Book")
    val first = File(folder, "1.mp3").apply { writeText("audio") }
    File(folder, "2.mp3").writeText("audio")

    scan(book(folder))

    coVerify(exactly = 1) { val _ = coverExtractor.extractCover(any(), any()) }
    coVerify { val _ = coverExtractor.extractCover(first.toUri(), image) }
  }

  @Test
  fun `an embedded cover becomes the cover`() = runTest {
    val folder = testFolder.newFolder("Book")
    File(folder, "1.mp3").writeText("audio")
    coEvery { coverExtractor.extractCover(any(), any()) } answers {
      secondArg<File>().writeText("embedded")
      true
    }

    scan(book(folder))

    assertEquals(expected = listOf("embedded"), actual = savedImages)
  }

  private suspend fun scan(
    book: Book,
    scanner: CoverScanner = this.scanner,
  ) {
    scanner.scan(listOf(book))
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
      .sortedBy { it.name }
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
