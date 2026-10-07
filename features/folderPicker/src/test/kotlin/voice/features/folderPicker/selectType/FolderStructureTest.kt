package voice.features.folderPicker.selectType

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import voice.core.documentfile.FileBasedDocumentFile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class FolderStructureTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `a folder of chapter files is one book`() {
    assertGuess(FolderMode.SingleBook, "01.mp3", "02.mp3", "03.mp3")
  }

  @Test
  fun `a folder split into discs is one book`() {
    assertGuess(FolderMode.SingleBook, "CD 1/01.mp3", "CD 2/01.mp3", "My Book - Disc2/01.mp3")
  }

  @Test
  fun `a folder named audiobooks with chapter files is still one book`() {
    assertGuess(FolderMode.SingleBook, "1.mp3", "2.mp3", folderName = "Audiobooks")
  }

  @Test
  fun `chapter files with a bonus folder are one book`() {
    assertGuess(FolderMode.SingleBook, "01.mp3", "02.mp3", "03.mp3", "Bonus/interview.mp3")
  }

  @Test
  fun `several audiobook files are a library`() {
    assertGuess(FolderMode.Audiobooks, "Dune.m4b", "Hyperion.m4b")
  }

  @Test
  fun `a single audiobook file is a library`() {
    assertGuess(FolderMode.Audiobooks, "Dune.m4b")
  }

  @Test
  fun `audiobook files with a stray chapter file are a library`() {
    assertGuess(FolderMode.Audiobooks, "Dune.m4b", "Hyperion.m4b", "intro.mp3")
  }

  @Test
  fun `book folders are a library`() {
    assertGuess(FolderMode.Audiobooks, "Dune/01.mp3", "Hyperion/01.mp3", "Hyperion/CD 2/01.mp3", "Neuromancer.m4b")
  }

  @Test
  fun `folders of book folders are authors`() {
    assertGuess(
      FolderMode.Authors,
      "Frank Herbert/Dune/01.mp3",
      "Frank Herbert/Children of Dune/01.mp3",
      "Dan Simmons/Hyperion/01.mp3",
    )
  }

  @Test
  fun `a book that looks like several books is flagged`() {
    val folder = folder("Discworld/Mort/1.mp3", "Discworld/Eric/1.mp3", "Dune/1.mp3", "Dune/CD 2/1.mp3")

    val books = folder.books(FolderMode.Audiobooks).associateBy { it.name }

    assertEquals(expected = 2, actual = books.getValue("Discworld").possibleBookCount)
    assertEquals(expected = 0, actual = books.getValue("Dune").possibleBookCount)
    assertEquals(expected = 1, actual = books.getValue("Dune").partCount)
  }

  @Test
  fun `books in author mode carry their author`() {
    val folder = folder("Dan Simmons/Hyperion/01.mp3", "Dan Simmons/Hyperion/02.mp3", "Loose.m4b")

    assertEquals(
      expected = listOf(
        SelectFolderTypeViewState.Book("Hyperion", author = "Dan Simmons", fileCount = 2, partCount = 0, possibleBookCount = 0),
        SelectFolderTypeViewState.Book("Loose", author = null, fileCount = 1, partCount = 0, possibleBookCount = 0),
      ),
      actual = folder.books(FolderMode.Authors).sortedBy { it.name },
    )
  }

  @Test
  fun `folders without audio are no books`() {
    val folder = folder("Dune/01.mp3", "Covers/dune.jpg")

    assertEquals(expected = listOf("Dune"), actual = folder.books(FolderMode.Audiobooks).map { it.name })
  }

  private fun assertGuess(
    expected: FolderMode,
    vararg files: String,
    folderName: String = "folder",
  ) {
    assertEquals(expected = expected, actual = folder(*files, folderName = folderName).guessFolderMode())
  }

  private fun folder(
    vararg files: String,
    folderName: String = "folder",
  ): FileBasedDocumentFile {
    val root = temporaryFolder.newFolder(folderName)
    files.forEach { path ->
      File(root, path).apply {
        parentFile!!.mkdirs()
        createNewFile()
      }
    }
    return FileBasedDocumentFile(root)
  }
}
