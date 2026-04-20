package voice.core.scanner

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.documentfile.FileBasedDocumentFile
import java.io.File
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class BookParserTest {

  @get:Rule
  val testFolder = TemporaryFolder()

  private val parser = BookParser(
    contentRepo = mockk(),
    mediaAnalyzer = mockk(),
    fileFactory = mockk(),
  )

  @Test
  fun folderBookUsesFolderNameWhenAlbumMissing() {
    val bookFolder = testFolder.newFolder("My Audiobook")
    val chapters = listOf(
      chapter(File(bookFolder, "1.mp3").apply { createNewFile() }),
      chapter(File(bookFolder, "2.mp3").apply { createNewFile() }),
    )

    val content = parser.parse(
      chapters = chapters,
      id = BookId(bookFolder.toUri()),
      analyzed = metadata(album = null, title = "First Chapter Title"),
      file = FileBasedDocumentFile(bookFolder),
    )

    content.name shouldBe "My Audiobook"
  }

  @Test
  fun folderBookWithSingleChapterStillUsesFolderName() {
    val bookFolder = testFolder.newFolder("Harry Potter 3")
    val chapters = listOf(chapter(File(bookFolder, "track01.mp3").apply { createNewFile() }))

    val content = parser.parse(
      chapters = chapters,
      id = BookId(bookFolder.toUri()),
      analyzed = metadata(album = null, title = "Track Title"),
      file = FileBasedDocumentFile(bookFolder),
    )

    content.name shouldBe "Harry Potter 3"
  }

  @Test
  fun singleFileBookUsesTitleWhenAlbumMissing() {
    val bookFile = testFolder.newFile("book.mp3")
    val chapters = listOf(chapter(bookFile))

    val content = parser.parse(
      chapters = chapters,
      id = BookId(bookFile.toUri()),
      analyzed = metadata(album = null, title = "The Title"),
      file = FileBasedDocumentFile(bookFile),
    )

    content.name shouldBe "The Title"
  }

  @Test
  fun albumAlwaysWinsOverTitleAndFolderName() {
    val bookFolder = testFolder.newFolder("Folder Name")
    val chapters = listOf(
      chapter(File(bookFolder, "1.mp3").apply { createNewFile() }),
      chapter(File(bookFolder, "2.mp3").apply { createNewFile() }),
    )

    val content = parser.parse(
      chapters = chapters,
      id = BookId(bookFolder.toUri()),
      analyzed = metadata(album = "Album Name", title = "First Chapter Title"),
      file = FileBasedDocumentFile(bookFolder),
    )

    content.name shouldBe "Album Name"
  }

  @Test
  fun missingMetadataFallsBackToFolderName() {
    val bookFolder = testFolder.newFolder("Fallback Folder")
    val chapters = listOf(
      chapter(File(bookFolder, "1.mp3").apply { createNewFile() }),
      chapter(File(bookFolder, "2.mp3").apply { createNewFile() }),
    )

    val content = parser.parse(
      chapters = chapters,
      id = BookId(bookFolder.toUri()),
      analyzed = null,
      file = FileBasedDocumentFile(bookFolder),
    )

    content.name shouldBe "Fallback Folder"
  }

  private fun chapter(file: File): Chapter = Chapter(
    id = ChapterId(file.toUri()),
    name = "Chapter",
    duration = 1000L,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
  )

  private fun metadata(
    album: String?,
    title: String?,
  ): Metadata = Metadata(
    duration = 1000L,
    artist = null,
    album = album,
    title = title,
    fileName = "file",
    chapters = emptyList(),
    genre = null,
    narrator = null,
    series = null,
    part = null,
  )
}
